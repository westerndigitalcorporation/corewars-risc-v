/* dwarf2dbg.c - DWARF2 debug support
   Copyright (C) 1999-2018 Free Software Foundation, Inc.
   Contributed by David Mosberger-Tang <davidm@hpl.hp.com>

   This file is part of GAS, the GNU Assembler.

   GAS is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3, or (at your option)
   any later version.

   GAS is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GAS; see the file COPYING.  If not, write to the Free
   Software Foundation, 51 Franklin Street - Fifth Floor, Boston, MA
   02110-1301, USA.  */

/* Logical line numbers can be controlled by the compiler via the
   following directives:

	.file FILENO "file.c"
	.loc  FILENO LINENO [COLUMN] [basic_block] [prologue_end] \
	      [epilogue_begin] [is_stmt VALUE] [isa VALUE] \
	      [discriminator VALUE]
*/

#include "as.h"
#include "safe-ctype.h"

#ifdef HAVE_LIMITS_H
#include <limits.h>
#else
#ifdef HAVE_SYS_PARAM_H
#include <sys/param.h>
#endif
#ifndef INT_MAX
#define INT_MAX (int) (((unsigned) (-1)) >> 1)
#endif
#endif

#include "dwarf2dbg.h"
#include <filenames.h>

#ifdef HAVE_DOS_BASED_FILE_SYSTEM
/* We need to decide which character to use as a directory separator.
   Just because HAVE_DOS_BASED_FILE_SYSTEM is defined, it does not
   necessarily mean that the backslash character is the one to use.
   Some environments, eg Cygwin, can support both naming conventions.
   So we use the heuristic that we only need to use the backslash if
   the path is an absolute path starting with a DOS style drive
   selector.  eg C: or D:  */
# define INSERT_DIR_SEPARATOR(string, offset) \
  do \
    { \
      if (offset > 1 \
	  && string[0] != 0 \
	  && string[1] == ':') \
       string [offset] = '\\'; \
      else \
       string [offset] = '/'; \
    } \
  while (0)
#else
# define INSERT_DIR_SEPARATOR(string, offset) string[offset] = '/'
#endif

#ifndef DWARF2_FORMAT
# define DWARF2_FORMAT(SEC) dwarf2_format_32bit
#endif

#ifndef DWARF2_ADDR_SIZE
# define DWARF2_ADDR_SIZE(bfd) (bfd_arch_bits_per_address (bfd) / 8)
#endif

#ifndef DWARF2_FILE_NAME
#define DWARF2_FILE_NAME(FILENAME, DIRNAME) FILENAME
#endif

#ifndef DWARF2_FILE_TIME_NAME
#define DWARF2_FILE_TIME_NAME(FILENAME,DIRNAME) 0
#endif

#ifndef DWARF2_FILE_SIZE_NAME
#define DWARF2_FILE_SIZE_NAME(FILENAME,DIRNAME) 0
#endif

#ifndef DWARF2_VERSION
#define DWARF2_VERSION 2
#endif

/* The .debug_aranges version has been 2 in DWARF version 2, 3 and 4. */
#ifndef DWARF2_ARANGES_VERSION
#define DWARF2_ARANGES_VERSION 2
#endif

/* This implementation output version 2 .debug_line information. */
#ifndef DWARF2_LINE_VERSION
#define DWARF2_LINE_VERSION 2
#endif

#include "subsegs.h"

#include "dwarf2.h"

/* Since we can't generate the prolog until the body is complete, we
   use three different subsegments for .debug_line: one holding the
   prolog, one for the directory and filename info, and one for the
   body ("statement program").  */
#define DL_PROLOG	0
#define DL_FILES	1
#define DL_BODY		2

/* If linker relaxation might change offsets in the code, the DWARF special
   opcodes and variable-length operands cannot be used.  If this macro is
   nonzero, use the DW_LNS_fixed_advance_pc opcode instead.  */
#ifndef DWARF2_USE_FIXED_ADVANCE_PC
# define DWARF2_USE_FIXED_ADVANCE_PC	linkrelax
#endif

/* First special line opcode - leave room for the standard opcodes.
   Note: If you want to change this, you'll have to update the
   "standard_opcode_lengths" table that is emitted below in
   out_debug_line().  */
#define DWARF2_LINE_OPCODE_BASE		13

#ifndef DWARF2_LINE_BASE
  /* Minimum line offset in a special line info. opcode.  This value
     was chosen to give a reasonable range of values.  */
# define DWARF2_LINE_BASE		-5
#endif

/* Range of line offsets in a special line info. opcode.  */
#ifndef DWARF2_LINE_RANGE
# define DWARF2_LINE_RANGE		14
#endif

#ifndef DWARF2_LINE_MIN_INSN_LENGTH
  /* Define the architecture-dependent minimum instruction length (in
     bytes).  This value should be rather too small than too big.  */
# define DWARF2_LINE_MIN_INSN_LENGTH	1
#endif

/* Flag that indicates the initial value of the is_stmt_start flag.  */
#define	DWARF2_LINE_DEFAULT_IS_STMT	1

/* Given a special op, return the line skip amount.  */
#define SPECIAL_LINE(op) \
	(((op) - DWARF2_LINE_OPCODE_BASE)%DWARF2_LINE_RANGE + DWARF2_LINE_BASE)

/* Given a special op, return the address skip amount (in units of
   DWARF2_LINE_MIN_INSN_LENGTH.  */
#define SPECIAL_ADDR(op) (((op) - DWARF2_LINE_OPCODE_BASE)/DWARF2_LINE_RANGE)

/* The maximum address skip amount that can be encoded with a special op.  */
#define MAX_SPECIAL_ADDR_DELTA		SPECIAL_ADDR(255)

#ifndef TC_PARSE_CONS_RETURN_NONE
#define TC_PARSE_CONS_RETURN_NONE BFD_RELOC_NONE
#endif

struct line_entry
{
  struct line_entry *next;
  symbolS *label;
  struct dwarf2_line_info loc;
};

/* Don't change the offset of next in line_entry.  set_or_check_view
   calls in dwarf2_gen_line_info_1 depend on it.  */
//static char unused[offsetof(struct line_entry, next) ? -1 : 1]
//ATTRIBUTE_UNUSED;

struct line_subseg
{
  struct line_subseg *next;
  subsegT subseg;
  struct line_entry *head;
  struct line_entry **ptail;
  struct line_entry **pmove_tail;
};

struct line_seg
{
  struct line_seg *next;
  segT seg;
  struct line_subseg *head;
  symbolS *text_start;
  symbolS *text_end;
};

/* Collects data for all line table entries during assembly.  */
static struct line_seg *all_segs;
static struct line_seg **last_seg_ptr;

struct file_entry
{
  const char *filename;
  unsigned int dir;
};

/* Table of files used by .debug_line.  */
static struct file_entry *files;
static unsigned int files_in_use;
static unsigned int files_allocated;

/* Table of directories used by .debug_line.  */
static char **dirs;
static unsigned int dirs_in_use;
static unsigned int dirs_allocated;

/* TRUE when we've seen a .loc directive recently.  Used to avoid
   doing work when there's nothing to do.  */
bfd_boolean dwarf2_loc_directive_seen;

/* TRUE when we're supposed to set the basic block mark whenever a
   label is seen.  */
bfd_boolean dwarf2_loc_mark_labels;

/* Current location as indicated by the most recent .loc directive.  */
static struct dwarf2_line_info current =
{
  1, 1, 0, 0,
  DWARF2_LINE_DEFAULT_IS_STMT ? DWARF2_FLAG_IS_STMT : 0,
  0, NULL
};

/* This symbol is used to recognize view number forced resets in loc
   lists.  */
static symbolS *force_reset_view;

/* This symbol evaluates to an expression that, if nonzero, indicates
   some view assert check failed.  */
static symbolS *view_assert_failed;

/* The size of an address on the target.  */
static unsigned int sizeof_address;

static unsigned int get_filenum (const char *, unsigned int);

#ifndef TC_DWARF2_EMIT_OFFSET
#define TC_DWARF2_EMIT_OFFSET  generic_dwarf2_emit_offset

/* Create an offset to .dwarf2_*.  */

static void
generic_dwarf2_emit_offset (symbolS *symbol, unsigned int size)
{
  expressionS exp;

  exp.X_op = O_symbol;
  exp.X_add_symbol = symbol;
  exp.X_add_number = 0;
  emit_expr (&exp, size);
}
#endif

/* Find or create (if CREATE_P) an entry for SEG+SUBSEG in ALL_SEGS.  */

static struct line_subseg *
get_line_subseg (segT seg, subsegT subseg, bfd_boolean create_p)
{
  struct line_seg *s = seg_info (seg)->dwarf2_line_seg;
  struct line_subseg **pss, *lss;

  if (s == NULL)
    {
      if (!create_p)
	return NULL;

      s = XNEW (struct line_seg);
      s->next = NULL;
      s->seg = seg;
      s->head = NULL;
      *last_seg_ptr = s;
      last_seg_ptr = &s->next;
      seg_info (seg)->dwarf2_line_seg = s;
    }
  gas_assert (seg == s->seg);

  for (pss = &s->head; (lss = *pss) != NULL ; pss = &lss->next)
    {
      if (lss->subseg == subseg)
	goto found_subseg;
      if (lss->subseg > subseg)
	break;
    }

  lss = XNEW (struct line_subseg);
  lss->next = *pss;
  lss->subseg = subseg;
  lss->head = NULL;
  lss->ptail = &lss->head;
  lss->pmove_tail = &lss->head;
  *pss = lss;

 found_subseg:
  return lss;
}

/* (Un)reverse the line_entry list starting from H.  */

static struct line_entry *
reverse_line_entry_list (struct line_entry *h)
{
  struct line_entry *p = NULL, *e, *n;

  for (e = h; e; e = n)
    {
      n = e->next;
      e->next = p;
      p = e;
    }
  return p;
}

/* Compute the view for E based on the previous entry P.  If we
   introduce an (undefined) view symbol for P, and H is given (P must
   be the tail in this case), introduce view symbols for earlier list
   entries as well, until one of them is constant.  */

static void
set_or_check_view (struct line_entry *e, struct line_entry *p,
		   struct line_entry *h)
{
  expressionS viewx;

  memset (&viewx, 0, sizeof (viewx));
  viewx.X_unsigned = 1;

  /* First, compute !(E->label > P->label), to tell whether or not
     we're to reset the view number.  If we can't resolve it to a
     constant, keep it symbolic.  */
  if (!p || (e->loc.view == force_reset_view && force_reset_view))
    {
      viewx.X_op = O_constant;
      viewx.X_add_number = 0;
      viewx.X_add_symbol = NULL;
      viewx.X_op_symbol = NULL;
    }
  else
    {
      viewx.X_op = O_gt;
      viewx.X_add_number = 0;
      viewx.X_add_symbol = e->label;
      viewx.X_op_symbol = p->label;
      resolve_expression (&viewx);
      if (viewx.X_op == O_constant)
	viewx.X_add_number = !viewx.X_add_number;
      else
	{
	  viewx.X_add_symbol = make_expr_symbol (&viewx);
	  viewx.X_add_number = 0;
	  viewx.X_op_symbol = NULL;
	  viewx.X_op = O_logical_not;
	}
    }

  if (S_IS_DEFINED (e->loc.view) && symbol_constant_p (e->loc.view))
    {
      expressionS *value = symbol_get_value_expression (e->loc.view);
      /* We can't compare the view numbers at this point, because in
	 VIEWX we've only determined whether we're to reset it so
	 far.  */
      if (viewx.X_op == O_constant)
	{
	  if (!value->X_add_number != !viewx.X_add_number)
	    as_bad (_("view number mismatch"));
	}
      /* Record the expression to check it later.  It is the result of
	 a logical not, thus 0 or 1.  We just add up all such deferred
	 expressions, and resolve it at the end.  */
      else if (!value->X_add_number)
	{
	  symbolS *deferred = make_expr_symbol (&viewx);
	  if (view_assert_failed)
	    {
	      expressionS chk;
	      memset (&chk, 0, sizeof (chk));
	      chk.X_unsigned = 1;
	      chk.X_op = O_add;
	      chk.X_add_number = 0;
	      chk.X_add_symbol = view_assert_failed;
	      chk.X_op_symbol = deferred;
	      deferred = make_expr_symbol (&chk);
	    }
	  view_assert_failed = deferred;
	}
    }

  if (viewx.X_op != O_constant || viewx.X_add_number)
    {
      expressionS incv;

      if (!p->loc.view)
	{
	  p->loc.view = symbol_temp_make ();
	  gas_assert (!S_IS_DEFINED (p->loc.view));
	}

      memset (&incv, 0, sizeof (incv));
      incv.X_unsigned = 1;
      incv.X_op = O_symbol;
      incv.X_add_symbol = p->loc.view;
      incv.X_add_number = 1;

      if (viewx.X_op == O_constant)
	{
	  gas_assert (viewx.X_add_number == 1);
	  viewx = incv;
	}
      else
	{
	  viewx.X_add_symbol = make_expr_symbol (&viewx);
	  viewx.X_add_number = 0;
	  viewx.X_op_symbol = make_expr_symbol (&incv);
	  viewx.X_op = O_multiply;
	}
    }

  if (!S_IS_DEFINED (e->loc.view))
    {
      symbol_set_value_expression (e->loc.view, &viewx);
      S_SET_SEGMENT (e->loc.view, absolute_section);
      symbol_set_frag (e->loc.view, &zero_address_frag);
    }

  /* Define and attempt to simplify any earlier views needed to
     compute E's.  */
  if (h && p && p->loc.view && !S_IS_DEFINED (p->loc.view))
    {
      struct line_entry *h2;
      /* Reverse the list to avoid quadratic behavior going backwards
	 in a single-linked list.  */
      struct line_entry *r = reverse_line_entry_list (h);

      gas_assert (r == p);
      /* Set or check views until we find a defined or absent view.  */
      do
	set_or_check_view (r, r->next, NULL);
      while (r->next && r->next->loc.view && !S_IS_DEFINED (r->next->loc.view)
	     && (r = r->next));

      /* Unreverse the list, so that we can go forward again.  */
      h2 = reverse_line_entry_list (p);
      gas_assert (h2 == h);

      /* Starting from the last view we just defined, attempt to
	 simplify the view expressions, until we do so to P.  */
      do
	{
	  gas_assert (S_IS_DEFINED (r->loc.view));
	  resolve_expression (symbol_get_value_expression (r->loc.view));
	}
      while (r != p && (r = r->next));

      /* Now that we've defined and computed all earlier views that might
	 be needed to compute E's, attempt to simplify it.  */
      resolve_expression (symbol_get_value_expression (e->loc.view));
    }
}

/* Record an entry for LOC occurring at LABEL.  */

static void
dwarf2_gen_line_info_1 (symbolS *label, struct dwarf2_line_info *loc)
{
  struct line_subseg *lss;
  struct line_entry *e;

  e = XNEW (struct line_entry);
  e->next = NULL;
  e->label = label;
  e->loc = *loc;

  lss = get_line_subseg (now_seg, now_subseg, TRUE);

  if (loc->view)
    set_or_check_view (e,
		       !lss->head ? NULL : (struct line_entry *)lss->ptail,
		       lss->head);

  *lss->ptail = e;
  lss->ptail = &e->next;
}

/* Record an entry for LOC occurring at OFS within the current fragment.  */

void
dwarf2_gen_line_info (addressT ofs, struct dwarf2_line_info *loc)
{
  static unsigned int line = -1;
  static unsigned int filenum = -1;

  symbolS *sym;

  /* Early out for as-yet incomplete location information.  */
  if (loc->filenum == 0 || loc->line == 0)
    return;

  /* Don't emit sequences of line symbols for the same line when the
     symbols apply to assembler code.  It is necessary to emit
     duplicate line symbols when a compiler asks for them, because GDB
     uses them to determine the end of the prologue.  */
  if (debug_type == DEBUG_DWARF2
      && line == loc->line && filenum == loc->filenum)
    return;

  line = loc->line;
  filenum = loc->filenum;

  if (linkrelax)
    {
      char name[120];

      /* Use a non-fake name for the line number location,
	 so that it can be referred to by relocations.  */
      sprintf (name, ".Loc.%u.%u", line, filenum);
      sym = symbol_new (name, now_seg, ofs, frag_now);
    }
  else
    sym = symbol_temp_new (now_seg, ofs, frag_now);
  dwarf2_gen_line_info_1 (sym, loc);
}

/* Returns the current source information.  If .file directives have
   been encountered, the info for the corresponding source file is
   returned.  Otherwise, the info for the assembly source file is
   returned.  */

void
dwarf2_where (struct dwarf2_line_info *line)
{
  if (debug_type == DEBUG_DWARF2)
    {
      const char *filename;

      memset (line, 0, sizeof (*line));
      filename = as_where (&line->line);
      line->filenum = get_filenum (filename, 0);
      line->column = 0;
      line->flags = DWARF2_FLAG_IS_STMT;
      line->isa = current.isa;
      line->discriminator = current.discriminator;
      line->view = NULL;
    }
  else
    *line = current;
}

/* A hook to allow the target backend to inform the line number state
   machine of isa changes when assembler debug info is enabled.  */

void
dwarf2_set_isa (unsigned int isa)
{
  current.isa = isa;
}

/* Called for each machine instruction, or relatively atomic group of
   machine instructions (ie built-in macro).  The instruction or group
   is SIZE bytes in length.  If dwarf2 line number generation is called
   for, emit a line statement appropriately.  */

void
dwarf2_emit_insn (int size)
{
  struct dwarf2_line_info loc;

  if (debug_type != DEBUG_DWARF2
      ? !dwarf2_loc_directive_seen
      : !seen_at_least_1_file ())
    return;

  dwarf2_where (&loc);

  dwarf2_gen_line_info (frag_now_fix () - size, &loc);
  dwarf2_consume_line_info ();
}

/* Move all previously-emitted line entries for the current position by
   DELTA bytes.  This function cannot be used to move the same entries
   twice.  */

void
dwarf2_move_insn (int delta)
{
  struct line_subseg *lss;
  struct line_entry *e;
  valueT now;

  if (delta == 0)
    return;

  lss = get_line_subseg (now_seg, now_subseg, FALSE);
  if (!lss)
    return;

  now = frag_now_fix ();
  while ((e = *lss->pmove_tail))
    {
      if (S_GET_VALUE (e->label) == now)
	S_SET_VALUE (e->label, now + delta);
      lss->pmove_tail = &e->next;
    }
}

/* Called after the current line information has been either used with
   dwarf2_gen_line_info or saved with a machine instruction for later use.
   This resets the state of the line number information to reflect that
   it has been used.  */

void
dwarf2_consume_line_info (void)
{
  /* Unless we generate DWARF2 debugging information for each
     assembler line, we only emit one line symbol for one LOC.  */
  dwarf2_loc_directive_seen = FALSE;

  current.flags &= ~(DWARF2_FLAG_BASIC_BLOCK
		     | DWARF2_FLAG_PROLOGUE_END
		     | DWARF2_FLAG_EPILOGUE_BEGIN);
  current.discriminator = 0;
  current.view = NULL;
}

/* Called for each (preferably code) label.  If dwarf2_loc_mark_labels
   is enabled, emit a basic block marker.  */

void
dwarf2_emit_label (symbolS *label)
{
  struct dwarf2_line_info loc;

  if (!dwarf2_loc_mark_labels)
    return;
  if (S_GET_SEGMENT (label) != now_seg)
    return;
  if (!(bfd_get_section_flags (stdoutput, now_seg) & SEC_CODE))
    return;
  if (files_in_use == 0 && debug_type != DEBUG_DWARF2)
    return;

  dwarf2_where (&loc);

  loc.flags |= DWARF2_FLAG_BASIC_BLOCK;

  dwarf2_gen_line_info_1 (label, &loc);
  dwarf2_consume_line_info ();
}

/* Get a .debug_line file number for FILENAME.  If NUM is nonzero,
   allocate it on that file table slot, otherwise return the first
   empty one.  */

static unsigned int
get_filenum (const char *filename, unsigned int num)
{
  static unsigned int last_used, last_used_dir_len;
  const char *file;
  size_t dir_len;
  unsigned int i, dir;

  if (num == 0 && last_used)
    {
      if (! files[last_used].dir
	  && filename_cmp (filename, files[last_used].filename) == 0)
	return last_used;
      if (files[last_used].dir
	  && filename_ncmp (filename, dirs[files[last_used].dir],
			    last_used_dir_len) == 0
	  && IS_DIR_SEPARATOR (filename [last_used_dir_len])
	  && filename_cmp (filename + last_used_dir_len + 1,
			   files[last_used].filename) == 0)
	return last_used;
    }

  file = lbasename (filename);
  /* Don't make empty string from / or A: from A:/ .  */
#ifdef HAVE_DOS_BASED_FILE_SYSTEM
  if (file <= filename + 3)
    file = filename;
#else
  if (file == filename + 1)
    file = filename;
#endif
  dir_len = file - filename;

  dir = 0;
  if (dir_len)
    {
#ifndef DWARF2_DIR_SHOULD_END_WITH_SEPARATOR
      --dir_len;
#endif
      for (dir = 1; dir < dirs_in_use; ++dir)
	if (filename_ncmp (filename, dirs[dir], dir_len) == 0
	    && dirs[dir][dir_len] == '\0')
	  break;

      if (dir >= dirs_in_use)
	{
	  if (dir >= dirs_allocated)
	    {
	      dirs_allocated = dir + 32;
	      dirs = XRESIZEVEC (char *, dirs, dirs_allocated);
	    }

	  dirs[dir] = xmemdup0 (filename, dir_len);
	  dirs_in_use = dir + 1;
	}
    }

  if (num == 0)
    {
      for (i = 1; i < files_in_use; ++i)
	if (files[i].dir == dir
	    && files[i].filename
	    && filename_cmp (file, files[i].filename) == 0)
	  {
	    last_used = i;
	    last_used_dir_len = dir_len;
	    return i;
	  }
    }
  else
    i = num;

  if (i >= files_allocated)
    {
      unsigned int old = files_allocated;

      files_allocated = i + 32;
      files = XRESIZEVEC (struct file_entry, files, files_allocated);

      memset (files + old, 0, (i + 32 - old) * sizeof (struct file_entry));
    }

  files[i].filename = num ? file : xstrdup (file);
  files[i].dir = dir;
  if (files_in_use < i + 1)
    files_in_use = i + 1;
  last_used = i;
  last_used_dir_len = dir_len;

  return i;
}

/* Handle two forms of .file directive:
   - Pass .file "source.c" to s_app_file
   - Handle .file 1 "source.c" by adding an entry to the DWARF-2 file table

   If an entry is added to the file table, return a pointer to the filename.  */

char *
dwarf2_directive_filename (void)
{
  offsetT num;
  char *filename;
  int filename_len;

  /* Continue to accept a bare string and pass it off.  */
  SKIP_WHITESPACE ();
  if (*input_line_pointer == '"')
    {
      s_app_file (0);
      return NULL;
    }

  num = get_absolute_expression ();
  filename = demand_copy_C_string (&filename_len);
  if (filename == NULL)
    return NULL;
  demand_empty_rest_of_line ();

  if (num < 1)
    {
      as_bad (_("file number less than one"));
      return NULL;
    }

  /* A .file directive implies compiler generated debug information is
     being supplied.  Turn off gas generated debug info.  */
  debug_type = DEBUG_NONE;

  if (num < (int) files_in_use && files[num].filename != 0)
    {
      as_bad (_("file number %ld already allocated"), (long) num);
      return NULL;
    }

  get_filenum (filename, num);

  return filename;
}

/* Calls dwarf2_directive_filename, but discards its result.
   Used in pseudo-op tables where the function result is ignored.  */

void
dwarf2_directive_file (int dummy ATTRIBUTE_UNUSED)
{
  (void) dwarf2_directive_filename ();
}

void
dwarf2_directive_loc (int dummy ATTRIBUTE_UNUSED)
{
  offsetT filenum, line;

  /* If we see two .loc directives in a row, force the first one to be
     output now.  */
  if (dwarf2_loc_directive_seen)
    dwarf2_emit_insn (0);

  filenum = get_absolute_expression ();
  SKIP_WHITESPACE ();
  line = get_absolute_expression ();

  if (filenum < 1)
    {
      as_bad (_("file number less than one"));
      return;
    }
  if (filenum >= (int) files_in_use || files[filenum].filename == 0)
    {
      as_bad (_("unassigned file number %ld"), (long) filenum);
      return;
    }

  current.filenum = filenum;
  current.line = line;
  current.discriminator = 0;

#ifndef NO_LISTING
  if (listing)
    {
      if (files[filenum].dir)
	{
	  size_t dir_len = strlen (dirs[files[filenum].dir]);
	  size_t file_len = strlen (files[filenum].filename);
	  char *cp = XNEWVEC (char, dir_len + 1 + file_len + 1);

	  memcpy (cp, dirs[files[filenum].dir], dir_len);
	  INSERT_DIR_SEPARATOR (cp, dir_len);
	  memcpy (cp + dir_len + 1, files[filenum].filename, file_len);
	  cp[dir_len + file_len + 1] = '\0';
	  listing_source_file (cp);
	  free (cp);
	}
      else
	listing_source_file (files[filenum].filename);
      listing_source_line (line);
    }
#endif

  SKIP_WHITESPACE ();
  if (ISDIGIT (*input_line_pointer))
    {
      current.column = get_absolute_expression ();
      SKIP_WHITESPACE ();
    }

  while (ISALPHA (*input_line_pointer))
    {
      char *p, c;
      offsetT value;

      c = get_symbol_name (& p);

      if (strcmp (p, "basic_block") == 0)
	{
	  current.flags |= DWARF2_FLAG_BASIC_BLOCK;
	  *input_line_pointer = c;
	}
      else if (strcmp (p, "prologue_end") == 0)
	{
	  current.flags |= DWARF2_FLAG_PROLOGUE_END;
	  *input_line_pointer = c;
	}
      else if (strcmp (p, "epilogue_begin") == 0)
	{
	  current.flags |= DWARF2_FLAG_EPILOGUE_BEGIN;
	  *input_line_pointer = c;
	}
      else if (strcmp (p, "is_stmt") == 0)
	{
	  (void) restore_line_pointer (c);
	  value = get_absolute_expression ();
	  if (value == 0)
	    current.flags &= ~DWARF2_FLAG_IS_STMT;
	  else if (value == 1)
	    current.flags |= DWARF2_FLAG_IS_STMT;
	  else
	    {
	      as_bad (_("is_stmt value not 0 or 1"));
	      return;
	    }
	}
      else if (strcmp (p, "isa") == 0)
	{
	  (void) restore_line_pointer (c);
	  value = get_absolute_expression ();
	  if (value >= 0)
	    current.isa = value;
	  else
	    {
	      as_bad (_("isa number less than zero"));
	      return;
	    }
	}
      else if (strcmp (p, "discriminator") == 0)
	{
	  (void) restore_line_pointer (c);
	  value = get_absolute_expression ();
	  if (value >= 0)
	    current.discriminator = value;
	  else
	    {
	      as_bad (_("discriminator less than zero"));
	      return;
	    }
	}
      else if (strcmp (p, "view") == 0)
	{
	  symbolS *sym;

	  (void) restore_line_pointer (c);
	  SKIP_WHITESPACE ();

	  if (ISDIGIT (*input_line_pointer)
	      || *input_line_pointer == '-')
	    {
	      bfd_boolean force_reset = *input_line_pointer == '-';

	      value = get_absolute_expression ();
	      if (value != 0)
		{
		  as_bad (_("numeric view can only be asserted to zero"));
		  return;
		}
	      if (force_reset && force_reset_view)
		sym = force_reset_view;
	      else
		{
		  sym = symbol_temp_new (absolute_section, value,
					 &zero_address_frag);
		  if (force_reset)
		    force_reset_view = sym;
		}
	    }
	  else
	    {
	      char *name = read_symbol_name ();

	      if (!name)
		return;
	      sym = symbol_find_or_make (name);
	      if (S_IS_DEFINED (sym))
		{
		  if (!S_CAN_BE_REDEFINED (sym))
		    as_bad (_("symbol `%s' is already defined"), name);
		  else
		    sym = symbol_clone (sym, 1);
		  S_SET_SEGMENT (sym, undefined_section);
		  S_SET_VALUE (sym, 0);
		  symbol_set_frag (sym, &zero_address_frag);
		}
	    }
	  current.view = sym;
	}
      else
	{
	  as_bad (_("unknown .loc sub-directive `%s'"), p);
	  (void) restore_line_pointer (c);
	  return;
	}

      SKIP_WHITESPACE_AFTER_NAME ();
    }

  demand_empty_rest_of_line ();
  dwarf2_loc_directive_seen = TRUE;
  debug_type = DEBUG_NONE;

  /* If we were given a view id, emit the row right away.  */
  if (current.view)
    dwarf2_emit_insn (0);
}

void
dwarf2_directive_loc_mark_labels (int dummy ATTRIBUTE_UNUSED)
{
  offsetT value = get_absolute_expression ();

  if (value != 0 && value != 1)
    {
      as_bad (_("expected 0 or 1"));
      ignore_rest_of_line ();
    }
  else
    {
      dwarf2_loc_mark_labels = value != 0;
      demand_empty_rest_of_line ();
    }
}

static struct frag *
first_frag_for_seg (segT seg)
{
  return seg_info (seg)->frchainP->frch_root;
}

static struct frag *
last_frag_for_seg (segT seg)
{
  frchainS *f = seg_info (seg)->frchainP;

  while (f->frch_next != NULL)
    f = f->frch_next;

  return f->frch_last;
}

/* Emit a single byte into the current segment.  */

static inline void
out_byte (int byte)
{
  FRAG_APPEND_1_CHAR (byte);
}

/* Emit a statement program opcode into the current segment.  */

static inline void
out_opcode (int opc)
{
  out_byte (opc);
}

/* Emit a two-byte word into the current segment.  */

static inline void
out_two (int data)
{
  md_number_to_chars (frag_more (2), data, 2);
}

/* Emit a four byte word into the current segment.  */

static inline void
out_four (int data)
{
  md_number_to_chars (frag_more (4), data, 4);
}

/* Emit an unsigned "little-endian base 128" number.  */

static void
out_uleb128 (addressT value)
{
  output_leb128 (frag_more (sizeof_leb128 (value, 0)), value, 0);
}

/* Emit a signed "little-endian base 128" number.  */

static void
out_leb128 (addressT value)
{
  output_leb128 (frag_more (sizeof_leb128 (value, 1)), value, 1);
}

/* Emit a tuple for .debug_abbrev.  */

static inline void
out_abbrev (int name, int form)
{
  out_uleb128 (name);
  out_uleb128 (form);
}

/* Get the size of a fragment.  */

static offsetT
get_frag_fix (fragS *frag, segT seg)
{
  frchainS *fr;

  if (frag->fr_next)
    return frag->fr_fix;

  /* If a fragment is the last in the chain, special measures must be
     taken to find its size before relaxation, since it may be pending
     on some subsegment chain.  */
  for (fr = seg_info (seg)->frchainP; fr; fr = fr->frch_next)
    if (fr->frch_last == frag)
      return (char *) obstack_next_free (&fr->frch_obstack) - frag->fr_literal;

  abort ();
}

/* Set an absolute address (may result in a relocation entry).  */

static void
out_set_addr (symbolS *sym)
{
  expressionS exp;

  out_opcode (DW_LNS_extended_op);
  out_uleb128 (sizeof_address + 1);

  out_opcode (DW_LNE_set_address);
  exp.X_op = O_symbol;
  exp.X_add_symbol = sym;
  exp.X_add_number = 0;
  emit_expr (&exp, sizeof_address);
}

static void scale_addr_delta (addressT *);

static void
scale_addr_delta (addressT *addr_delta)
{
  static int printed_this = 0;
  if (DWARF2_LINE_MIN_INSN_LENGTH > 1)
    {
      if (*addr_delta % DWARF2_LINE_MIN_INSN_LENGTH != 0  && !printed_this)
        {
	  as_bad("unaligned opcodes detected in executable segment");
          printed_this = 1;
        }
      *addr_delta /= DWARF2_LINE_MIN_INSN_LENGTH;
    }
}

/* Encode a pair of line and address skips as efficiently as possible.
   Note that the line skip is signed, whereas the address skip is unsigned.

   The following two routines *must* be kept in sync.  This is
   enforced by making emit_inc_line_addr abort if we do not emit
   exactly the expected number of bytes.  */

static int
size_inc_line_addr (int line_delta, addressT addr_delta)
{
  unsigned int tmp, opcode;
  int len = 0;

  /* Scale the address delta by the minimum instruction length.  */
  scale_addr_delta (&addr_delta);

  /* INT_MAX is a signal that this is actually a DW_LNE_end_sequence.
     We cannot use special opcodes here, since we want the end_sequence
     to emit the matrix entry.  */
  if (line_delta == INT_MAX)
    {
      if (addr_delta == MAX_SPECIAL_ADDR_DELTA)
	len = 1;
      else
	len = 1 + sizeof_leb128 (addr_delta, 0);
      return len + 3;
    }

  /* Bias the line delta by the base.  */
  tmp = line_delta - DWARF2_LINE_BASE;

  /* If the line increment is out of range of a special opcode, we
     must encode it with DW_LNS_advance_line.  */
  if (tmp >= DWARF2_LINE_RANGE)
    {
      len = 1 + sizeof_leb128 (line_delta, 1);
      line_delta = 0;
      tmp = 0 - DWARF2_LINE_BASE;
    }

  /* Bias the opcode by the special opcode base.  */
  tmp += DWARF2_LINE_OPCODE_BASE;

  /* Avoid overflow when addr_delta is large.  */
  if (addr_delta < 256 + MAX_SPECIAL_ADDR_DELTA)
    {
      /* Try using a special opcode.  */
      opcode = tmp + addr_delta * DWARF2_LINE_RANGE;
      if (opcode <= 255)
	return len + 1;

      /* Try using DW_LNS_const_add_pc followed by special op.  */
      opcode = tmp + (addr_delta - MAX_SPECIAL_ADDR_DELTA) * DWARF2_LINE_RANGE;
      if (opcode <= 255)
	return len + 2;
    }

  /* Otherwise use DW_LNS_advance_pc.  */
  len += 1 + sizeof_leb128 (addr_delta, 0);

  /* DW_LNS_copy or special opcode.  */
  len += 1;

  return len;
}

static void
emit_inc_line_addr (int line_delta, addressT addr_delta, char *p, int len)
{
  unsigned int tmp, opcode;
  int need_copy = 0;
  char *end = p + len;

  /* Line number sequences cannot go backward in addresses.  This means
     we've incorrectly ordered the statements in the sequence.  */
  gas_assert ((offsetT) addr_delta >= 0);

  /* Scale the address delta by the minimum instruction length.  */
  scale_addr_delta (&addr_delta);

  /* INT_MAX is a signal that this is actually a DW_LNE_end_sequence.
     We cannot use special opcodes here, since we want the end_sequence
     to emit the matrix entry.  */
  if (line_delta == INT_MAX)
    {
      if (addr_delta == MAX_SPECIAL_ADDR_DELTA)
	*p++ = DW_LNS_const_add_pc;
      else
	{
	  *p++ = DW_LNS_advance_pc;
	  p += output_leb128 (p, addr_delta, 0);
	}

      *p++ = DW_LNS_extended_op;
      *p++ = 1;
      *p++ = DW_LNE_end_sequence;
      goto done;
    }

  /* Bias the line delta by the base.  */
  tmp = line_delta - DWARF2_LINE_BASE;

  /* If the line increment is out of range of a special opcode, we
     must encode it with DW_LNS_advance_line.  */
  if (tmp >= DWARF2_LINE_RANGE)
    {
      *p++ = DW_LNS_advance_line;
      p += output_leb128 (p, line_delta, 1);

      line_delta = 0;
      tmp = 0 - DWARF2_LINE_BASE;
      need_copy = 1;
    }

  /* Prettier, I think, to use DW_LNS_copy instead of a "line +0, addr +0"
     special opcode.  */
  if (line_delta == 0 && addr_delta == 0)
    {
      *p++ = DW_LNS_copy;
      goto done;
    }

  /* Bias the opcode by the special opcode base.  */
  tmp += DWARF2_LINE_OPCODE_BASE;

  /* Avoid overflow when addr_delta is large.  */
  if (addr_delta < 256 + MAX_SPECIAL_ADDR_DELTA)
    {
      /* Try using a special opcode.  */
      opcode = tmp + addr_delta * DWARF2_LINE_RANGE;
      if (opcode <= 255)
	{
	  *p++ = opcode;
	  goto done;
	}

      /* Try using DW_LNS_const_add_pc followed by special op.  */
      opcode = tmp + (addr_delta - MAX_SPECIAL_ADDR_DELTA) * DWARF2_LINE_RANGE;
      if (opcode <= 255)
	{
	  *p++ = DW_LNS_const_add_pc;
	  *p++ = opcode;
	  goto done;
	}
    }

  /* Otherwise use DW_LNS_advance_pc.  */
  *p++ = DW_LNS_advance_pc;
  p += output_leb128 (p, addr_delta, 0);

  if (need_copy)
    *p++ = DW_LNS_copy;
  else
    *p++ = tmp;

 done:
  gas_assert (p == end);
}

/* Handy routine to combine calls to the above two routines.  */

static void
out_inc_line_addr (int line_delta, addressT addr_delta)
{
  int len = size_inc_line_addr (line_delta, addr_delta);
  emit_inc_line_addr (line_delta, addr_delta, frag_more (len), len);
}

/* Write out an alternative form of line and address skips using
   DW_LNS_fixed_advance_pc opcodes.  This uses more space than the default
   line and address information, but it is required if linker relaxation
   could change the code offsets.  The following two routines *must* be
   kept in sync.  */
#define ADDR_DELTA_LIMIT 50000

static int
size_fixed_inc_line_addr (int line_delta, addressT addr_delta)
{
  int len = 0;

  /* INT_MAX is a signal that this is actually a DW_LNE_end_sequence.  */
  if (line_delta != INT_MAX)
    len = 1 + sizeof_leb128 (line_delta, 1);

  if (addr_delta > ADDR_DELTA_LIMIT)
    {
      /* DW_LNS_extended_op */
      len += 1 + sizeof_leb128 (sizeof_address + 1, 0);
      /* DW_LNE_set_address */
      len += 1 + sizeof_address;
    }
  else
    /* DW_LNS_fixed_advance_pc */
    len += 3;

  if (line_delta == INT_MAX)
    /* DW_LNS_extended_op + DW_LNE_end_sequence */
    len += 3;
  else
    /* DW_LNS_copy */
    len += 1;

  return len;
}

static void
emit_fixed_inc_line_addr (int line_delta, addressT addr_delta, fragS *frag,
			  char *p, int len)
{
  expressionS *pexp;
  char *end = p + len;

  /* Line number sequences cannot go backward in addresses.  This means
     we've incorrectly ordered the statements in the sequence.  */
  gas_assert ((offsetT) addr_delta >= 0);

  /* Verify that we have kept in sync with size_fixed_inc_line_addr.  */
  gas_assert (len == size_fixed_inc_line_addr (line_delta, addr_delta));

  /* INT_MAX is a signal that this is actually a DW_LNE_end_sequence.  */
  if (line_delta != INT_MAX)
    {
      *p++ = DW_LNS_advance_line;
      p += output_leb128 (p, line_delta, 1);
    }

  pexp = symbol_get_value_expression (frag->fr_symbol);

  /* The DW_LNS_fixed_advance_pc opcode has a 2-byte operand so it can
     advance the address by at most 64K.  Linker relaxation (without
     which this function would not be used) could change the operand by
     an unknown amount.  If the address increment is getting close to
     the limit, just reset the address.  */
  if (addr_delta > ADDR_DELTA_LIMIT)
    {
      symbolS *to_sym;
      expressionS exp;

      gas_assert (pexp->X_op == O_subtract);
      to_sym = pexp->X_add_symbol;

      *p++ = DW_LNS_extended_op;
      p += output_leb128 (p, sizeof_address + 1, 0);
      *p++ = DW_LNE_set_address;
      exp.X_op = O_symbol;
      exp.X_add_symbol = to_sym;
      exp.X_add_number = 0;
      emit_expr_fix (&exp, sizeof_address, frag, p, TC_PARSE_CONS_RETURN_NONE);
      p += sizeof_address;
    }
  else
    {
      *p++ = DW_LNS_fixed_advance_pc;
      emit_expr_fix (pexp, 2, frag, p, TC_PARSE_CONS_RETURN_NONE);
      p += 2;
    }

  if (line_delta == INT_MAX)
    {
      *p++ = DW_LNS_extended_op;
      *p++ = 1;
      *p++ = DW_LNE_end_sequence;
    }
  else
    *p++ = DW_LNS_copy;

  gas_assert (p == end);
}

/* Generate a variant frag that we can use to relax address/line
   increments between fragments of the target segment.  */

static void
relax_inc_line_addr (int line_delta, symbolS *to_sym, symbolS *from_sym)
{
  expressionS exp;
  int max_chars;

  exp.X_op = O_subtract;
  exp.X_add_symbol = to_sym;
  exp.X_op_symbol = from_sym;
  exp.X_add_number = 0;

  /* The maximum size of the frag is the line delta with a maximum
     sized address delta.  */
  if (DWARF2_USE_FIXED_ADVANCE_PC)
    max_chars = size_fixed_inc_line_addr (line_delta,
					  -DWARF2_LINE_MIN_INSN_LENGTH);
  else
    max_chars = size_inc_line_addr (line_delta, -DWARF2_LINE_MIN_INSN_LENGTH);

  frag_var (rs_dwarf2dbg, max_chars, max_chars, 1,
	    make_expr_symbol (&exp), line_delta, NULL);
}

/* The function estimates the size of a rs_dwarf2dbg variant frag
   based on the current values of the symbols.  It is called before
   the relaxation loop.  We set fr_subtype to the expected length.  */

int
dwarf2dbg_estimate_size_before_relax (fragS *frag)
{
  offsetT addr_delta;
  int size;

  addr_delta = resolve_symbol_value (frag->fr_symbol);
  if (DWARF2_USE_FIXED_ADVANCE_PC)
    size = size_fixed_inc_line_addr (frag->fr_offset, addr_delta);
  else
    size = size_inc_line_addr (frag->fr_offset, addr_delta);

  frag->fr_subtype = size;

  return size;
}

/* This function relaxes a rs_dwarf2dbg variant frag based on the
   current values of the symbols.  fr_subtype is the current length
   of the frag.  This returns the change in frag length.  */

int
dwarf2dbg_relax_frag (fragS *frag)
{
  int old_size, new_size;

  old_size = frag->fr_subtype;
  new_size = dwarf2dbg_estimate_size_before_relax (frag);

  return new_size - old_size;
}

/* This function converts a rs_dwarf2dbg variant frag into a normal
   fill frag.  This is called after all relaxation has been done.
   fr_subtype will be the desired length of the frag.  */

void
dwarf2dbg_convert_frag (fragS *frag)
{
  offsetT addr_diff;

  if (DWARF2_USE_FIXED_ADVANCE_PC)
    {
      /* If linker relaxation is enabled then the distance between the two
	 symbols in the frag->fr_symbol expression might change.  Hence we
	 cannot rely upon the value computed by resolve_symbol_value.
	 Instead we leave the expression unfinalized and allow
	 emit_fixed_inc_line_addr to create a fixup (which later becomes a
	 relocation) that will allow the linker to correctly compute the
	 actual address difference.  We have to use a fixed line advance for
	 this as we cannot (easily) relocate leb128 encoded values.  */
      int saved_finalize_syms = finalize_syms;

      finalize_syms = 0;
      addr_diff = resolve_symbol_value (frag->fr_symbol);
      finalize_syms = saved_finalize_syms;
    }
  else
    addr_diff = resolve_symbol_value (frag->fr_symbol);

  /* fr_var carries the max_chars that we created the fragment with.
     fr_subtype carries the current expected length.  We must, of
     course, have allocated enough memory earlier.  */
  gas_assert (frag->fr_var >= (int) frag->fr_subtype);

  if (DWARF2_USE_FIXED_ADVANCE_PC)
    emit_fixed_inc_line_addr (frag->fr_offset, addr_diff, frag,
			      frag->fr_literal + frag->fr_fix,
			      frag->fr_subtype);
  else
    emit_inc_line_addr (frag->fr_offset, addr_diff,
			frag->fr_literal + frag->fr_fix, frag->fr_subtype);

  frag->fr_fix += frag->fr_subtype;
  frag->fr_type = rs_fill;
  frag->fr_var = 0;
  frag->fr_offset = 0;
}

/* Generate .debug_line content for the chain of line number entries
   beginning at E, for segment SEG.  */

static void
process_entries (segT seg, struct line_entry *e)
{
  unsigned filenum = 1;
  unsigned line = 1;
  unsigned column = 0;
  unsigned isa = 0;
  unsigned flags = DWARF2_LINE_DEFAULT_IS_STMT ? DWARF2_FLAG_IS_STMT : 0;
  fragS *last_frag = NULL, *frag;
  addressT last_frag_ofs = 0, frag_ofs;
  symbolS *last_lab = NULL, *lab;
  struct line_entry *next;

  if (flag_dwarf_sections)
    {
      char * name;
      const char * sec_name;

      /* Switch to the relevant sub-section before we start to emit
	 the line number table.

	 FIXME: These sub-sections do not have a normal Line Number
	 Program Header, thus strictly speaking they are not valid
	 DWARF sections.  Unfortunately the DWARF standard assumes
	 a one-to-one relationship between compilation units and
	 line number tables.  Thus we have to have a .debug_line
	 section, as well as our sub-sections, and we have to ensure
	 that all of the sub-sections are merged into a proper
	 .debug_line section before a debugger sees them.  */

      sec_name = bfd_get_section_name (stdoutput, seg);
      if (strcmp (sec_name, ".text") != 0)
	{
	  name = concat (".debug_line", sec_name, (char *) NULL);
	  subseg_set (subseg_get (name, FALSE), 0);
	}
      else
	/* Don't create a .debug_line.text section -
	   that is redundant.  Instead just switch back to the
	   normal .debug_line section.  */
	subseg_set (subseg_get (".debug_line", FALSE), 0);
    }

  do
    {
      int line_delta;

      if (filenum != e->loc.filenum)
	{
	  filenum = e->loc.filenum;
	  out_opcode (DW_LNS_set_file);
	  out_uleb128 (filenum);
	}

      if (column != e->loc.column)
	{
	  column = e->loc.column;
	  out_opcode (DW_LNS_set_column);
	  out_uleb128 (column);
	}

      if (e->loc.discriminator != 0)
	{
	  out_opcode (DW_LNS_extended_op);
	  out_leb128 (1 + sizeof_leb128 (e->loc.discriminator, 0));
	  out_opcode (DW_LNE_set_discriminator);
	  out_uleb128 (e->loc.discriminator);
	}

      if (isa != e->loc.isa)
	{
	  isa = e->loc.isa;
	  out_opcode (DW_LNS_set_isa);
	  out_uleb128 (isa);
	}

      if ((e->loc.flags ^ flags) & DWARF2_FLAG_IS_STMT)
	{
	  flags = e->loc.flags;
	  out_opcode (DW_LNS_negate_stmt);
	}

      if (e->loc.flags & DWARF2_FLAG_BASIC_BLOCK)
	out_opcode (DW_LNS_set_basic_block);

      if (e->loc.flags & DWARF2_FLAG_PROLOGUE_END)
	out_opcode (DW_LNS_set_prologue_end);

      if (e->loc.flags & DWARF2_FLAG_EPILOGUE_BEGIN)
	out_opcode (DW_LNS_set_epilogue_begin);

      /* Don't try to optimize away redundant entries; gdb wants two
	 entries for a function where the code starts on the same line as
	 the {, and there's no way to identify that case here.  Trust gcc
	 to optimize appropriately.  */
      line_delta = e->loc.line - line;
      lab = e->label;
      frag = symbol_get_frag (lab);
      frag_ofs = S_GET_VALUE (lab);

      if (last_frag == NULL
	  || (e->loc.view == force_reset_view && force_reset_view
	      /* If we're going to reset the view, but we know we're
		 advancing the PC, we don't have to force with
		 set_address.  We know we do when we're at the same
		 address of the same frag, and we know we might when
		 we're in the beginning of a frag, and we were at the
		 end of the previous frag.  */
	      && (frag == last_frag
		  ? (last_frag_ofs == frag_ofs)
		  : (frag_ofs == 0
		     && ((offsetT)last_frag_ofs
			 >= get_frag_fix (last_frag, seg))))))
	{
	  out_set_addr (lab);
	  out_inc_line_addr (line_delta, 0);
	}
      else if (frag == last_frag && ! DWARF2_USE_FIXED_ADVANCE_PC)
	out_inc_line_addr (line_delta, frag_ofs - last_frag_ofs);
      else
	relax_inc_line_addr (line_delta, lab, last_lab);

      line = e->loc.line;
      last_lab = lab;
      last_frag = frag;
      last_frag_ofs = frag_ofs;

      next = e->next;
      free (e);
      e = next;
    }
  while (e);

  /* Emit a DW_LNE_end_sequence for the end of the section.  */
  frag = last_frag_for_seg (seg);
  frag_ofs = get_frag_fix (frag, seg);
  if (frag == last_frag && ! DWARF2_USE_FIXED_ADVANCE_PC)
    out_inc_line_addr (INT_MAX, frag_ofs - last_frag_ofs);
  else
    {
      lab = symbol_temp_new (seg, frag_ofs, frag);
      relax_inc_line_addr (INT_MAX, lab, last_lab);
    }
}

/* Emit the directory and file tables for .debug_line.  */

static void
out_file_list (void)
{
  size_t size;
  const char *dir;
  char *cp;
  unsigned int i;

  /* Emit directory list.  */
  for (i = 1; i < dirs_in_use; ++i)
    {
      dir = remap_debug_filename (dirs[i]);
      size = strlen (dir) + 1;
      cp = frag_more (size);
      memcpy (cp, dir, size);
    }
  /* Terminate it.  */
  out_byte ('\0');

  for (i = 1; i < files_in_use; ++i)
    {
      const char *fullfilename;

      if (files[i].filename == NULL)
	{
	  as_bad (_("unassigned file number %ld"), (long) i);
	  /* Prevent a crash later, particularly for file 1.  */
	  files[i].filename = "";
	  continue;
	}

      fullfilename = DWARF2_FILE_NAME (files[i].filename,
				       files[i].dir ? dirs [files [i].dir] : "");
      size = strlen (fullfilename) + 1;
      cp = frag_more (size);
      memcpy (cp, fullfilename, size);

      out_uleb128 (files[i].dir);	/* directory number */
      /* Output the last modification timestamp.  */
      out_uleb128 (DWARF2_FILE_TIME_NAME (files[i].filename,
				          files[i].dir ? dirs [files [i].dir] : ""));
      /* Output the filesize.  */
      out_uleb128 (DWARF2_FILE_SIZE_NAME (files[i].filename,
				          files[i].dir ? dirs [files [i].dir] : ""));
    }

  /* Terminate filename list.  */
  out_byte (0);
}

/* Switch to SEC and output a header length field.  Return the size of
   offsets used in SEC.  The caller must set EXPR->X_add_symbol value
   to the end of the section.  EXPR->X_add_number will be set to the
   negative size of the header.  */

static int
out_header (asection *sec, expressionS *exp)
{
  symbolS *start_sym;
  symbolS *end_sym;

  subseg_set (sec, 0);

  if (flag_dwarf_sections)
    {
      /* If we are going to put the start and end symbols in different
	 sections, then we need real symbols, not just fake, local ones.  */
      frag_now_fix ();
      start_sym = symbol_make (".Ldebug_line_start");
      end_sym = symbol_make (".Ldebug_line_end");
      symbol_set_value_now (start_sym);
    }
  else
    {
      start_sym = symbol_temp_new_now ();
      end_sym = symbol_temp_make ();
    }

  /* Total length of the information.  */
  exp->X_op = O_subtract;
  exp->X_add_symbol = end_sym;
  exp->X_op_symbol = start_sym;

  switch (DWARF2_FORMAT (sec))
    {
    case dwarf2_format_32bit:
      exp->X_add_number = -4;
      emit_expr (exp, 4);
      return 4;

    case dwarf2_format_64bit:
      exp->X_add_number = -12;
      out_four (-1);
      emit_expr (exp, 8);
      return 8;

    case dwarf2_format_64bit_irix:
      exp->X_add_number = -8;
      emit_expr (exp, 8);
      return 8;
    }

  as_fatal (_("internal error: unknown dwarf2 format"));
  return 0;
}

/* Emit the collected .debug_line data.  */

static void
out_debug_line (segT line_seg)
{
  expressionS exp;
  symbolS *prologue_start, *prologue_end;
  symbolS *line_end;
  struct line_seg *s;
  int sizeof_offset;

  sizeof_offset = out_header (line_seg, &exp);
  line_end = exp.X_add_symbol;

  /* Version.  */
  out_two (DWARF2_LINE_VERSION);

  /* Length of the prologue following this length.  */
  prologue_start = symbol_temp_make ();
  prologue_end = symbol_temp_make ();
  exp.X_op = O_subtract;
  exp.X_add_symbol = prologue_end;
  exp.X_op_symbol = prologue_start;
  exp.X_add_number = 0;
  emit_expr (&exp, sizeof_offset);
  symbol_set_value_now (prologue_start);

  /* Parameters of the state machine.  */
  out_byte (DWARF2_LINE_MIN_INSN_LENGTH);
  out_byte (DWARF2_LINE_DEFAULT_IS_STMT);
  out_byte (DWARF2_LINE_BASE);
  out_byte (DWARF2_LINE_RANGE);
  out_byte (DWARF2_LINE_OPCODE_BASE);

  /* Standard opcode lengths.  */
  out_byte (0);			/* DW_LNS_copy */
  out_byte (1);			/* DW_LNS_advance_pc */
  out_byte (1);			/* DW_LNS_advance_line */
  out_byte (1);			/* DW_LNS_set_file */
  out_byte (1);			/* DW_LNS_set_column */
  out_byte (0);			/* DW_LNS_negate_stmt */
  out_byte (0);			/* DW_LNS_set_basic_block */
  out_byte (0);			/* DW_LNS_const_add_pc */
  out_byte (1);			/* DW_LNS_fixed_advance_pc */
  out_byte (0);			/* DW_LNS_set_prologue_end */
  out_byte (0);			/* DW_LNS_set_epilogue_begin */
  out_byte (1);			/* DW_LNS_set_isa */

  out_file_list ();

  symbol_set_value_now (prologue_end);

  /* For each section, emit a statement program.  */
  for (s = all_segs; s; s = s->next)
    if (SEG_NORMAL (s->seg))
      process_entries (s->seg, s->head->head);
    else
      as_warn ("dwarf line number information for %s ignored",
	       segment_name (s->seg));

  if (flag_dwarf_sections)
    /* We have to switch to the special .debug_line_end section
       before emitting the end-of-debug_line symbol.  The linker
       script arranges for this section to be placed after all the
       (potentially garbage collected) .debug_line.<foo> sections.
       This section contains the line_end symbol which is used to
       compute the size of the linked .debug_line section, as seen
       in the DWARF Line Number header.  */
    subseg_set (subseg_get (".debug_line_end", FALSE), 0);

  symbol_set_value_now (line_end);
}

static void
out_debug_ranges (segT ranges_seg)
{
  unsigned int addr_size = sizeof_address;
  struct line_seg *s;
  expressionS exp;
  unsigned int i;

  subseg_set (ranges_seg, 0);

  /* Base Address Entry.  */
  for (i = 0; i < addr_size; i++)
    out_byte (0xff);
  for (i = 0; i < addr_size; i++)
    out_byte (0);

  /* Range List Entry.  */
  for (s = all_segs; s; s = s->next)
    {
      fragS *frag;
      symbolS *beg, *end;

      frag = first_frag_for_seg (s->seg);
      beg = symbol_temp_new (s->seg, 0, frag);
      s->text_start = beg;

      frag = last_frag_for_seg (s->seg);
      end = symbol_temp_new (s->seg, get_frag_fix (frag, s->seg), frag);
      s->text_end = end;

      exp.X_op = O_symbol;
      exp.X_add_symbol = beg;
      exp.X_add_number = 0;
      emit_expr (&exp, addr_size);

      exp.X_op = O_symbol;
      exp.X_add_symbol = end;
      exp.X_add_number = 0;
      emit_expr (&exp, addr_size);
    }

  /* End of Range Entry.   */
  for (i = 0; i < addr_size; i++)
    out_byte (0);
  for (i = 0; i < addr_size; i++)
    out_byte (0);
}

/* Emit data for .debug_aranges.  */

static void
out_debug_aranges (segT aranges_seg, segT info_seg)
{
  unsigned int addr_size = sizeof_address;
  offsetT size;
  struct line_seg *s;
  expressionS exp;
  symbolS *aranges_end;
  char *p;
  int sizeof_offset;

  sizeof_offset = out_header (aranges_seg, &exp);
  aranges_end = exp.X_add_symbol;
  size = -exp.X_add_number;

  /* Version.  */
  out_two (DWARF2_ARANGES_VERSION);
  size += 2;

  /* Offset to .debug_info.  */
  TC_DWARF2_EMIT_OFFSET (section_symbol (info_seg), sizeof_offset);
  size += sizeof_offset;

  /* Size of an address (offset portion).  */
  out_byte (addr_size);
  size++;

  /* Size of a segment descriptor.  */
  out_byte (0);
  size++;

  /* Align the header.  */
  while ((size++ % (2 * addr_size)) > 0)
    out_byte (0);

  for (s = all_segs; s; s = s->next)
    {
      fragS *frag;
      symbolS *beg, *end;

      frag = first_frag_for_seg (s->seg);
      beg = symbol_temp_new (s->seg, 0, frag);
      s->text_start = beg;

      frag = last_frag_for_seg (s->seg);
      end = symbol_temp_new (s->seg, get_frag_fix (frag, s->seg), frag);
      s->text_end = end;

      exp.X_op = O_symbol;
      exp.X_add_symbol = beg;
      exp.X_add_number = 0;
      emit_expr (&exp, addr_size);

      exp.X_op = O_subtract;
      exp.X_add_symbol = end;
      exp.X_op_symbol = beg;
      exp.X_add_number = 0;
      emit_expr (&exp, addr_size);
    }

  p = frag_more (2 * addr_size);
  md_number_to_chars (p, 0, addr_size);
  md_number_to_chars (p + addr_size, 0, addr_size);

  symbol_set_value_now (aranges_end);
}

/* Emit data for .debug_abbrev.  Note that this must be kept in
   sync with out_debug_info below.  */

static void
out_debug_abbrev (segT abbrev_seg,
		  segT info_seg ATTRIBUTE_UNUSED,
		  segT line_seg ATTRIBUTE_UNUSED)
{
  subseg_set (abbrev_seg, 0);

  out_uleb128 (1);
  out_uleb128 (DW_TAG_compile_unit);
  out_byte (DW_CHILDREN_no);
  if (DWARF2_FORMAT (line_seg) == dwarf2_format_32bit)
    out_abbrev (DW_AT_stmt_list, DW_FORM_data4);
  else
    out_abbrev (DW_AT_stmt_list, DW_FORM_data8);
  if (all_segs->next == NULL)
    {
      out_abbrev (DW_AT_low_pc, DW_FORM_addr);
      if (DWARF2_VERSION < 4)
	out_abbrev (DW_AT_high_pc, DW_FORM_addr);
      else
	out_abbrev (DW_AT_high_pc, (sizeof_address == 4
				    ? DW_FORM_data4 : DW_FORM_data8));
    }
  else
    {
      if (DWARF2_FORMAT (info_seg) == dwarf2_format_32bit)
	out_abbrev (DW_AT_ranges, DW_FORM_data4);
      else
	out_abbrev (DW_AT_ranges, DW_FORM_data8);
    }
  out_abbrev (DW_AT_name, DW_FORM_strp);
  out_abbrev (DW_AT_comp_dir, DW_FORM_strp);
  out_abbrev (DW_AT_producer, DW_FORM_strp);
  out_abbrev (DW_AT_language, DW_FORM_data2);
  out_abbrev (0, 0);

  /* Terminate the abbreviations for this compilation unit.  */
  out_byte (0);
}

/* Emit a description of this compilation unit for .debug_info.  */

static void
out_debug_info (segT info_seg, segT abbrev_seg, segT line_seg, segT ranges_seg,
		symbolS *name_sym, symbolS *comp_dir_sym, symbolS *producer_sym)
{
  expressionS exp;
  symbolS *info_end;
  int sizeof_offset;

  sizeof_offset = out_header (info_seg, &exp);
  info_end = exp.X_add_symbol;

  /* DWARF version.  */
  out_two (DWARF2_VERSION);

  /* .debug_abbrev offset */
  TC_DWARF2_EMIT_OFFSET (section_symbol (abbrev_seg), sizeof_offset);

  /* Target address size.  */
  out_byte (sizeof_address);

  /* DW_TAG_compile_unit DIE abbrev */
  out_uleb128 (1);

  /* DW_AT_stmt_list */
  TC_DWARF2_EMIT_OFFSET (section_symbol (line_seg),
			 (DWARF2_FORMAT (line_seg) == dwarf2_format_32bit
			  ? 4 : 8));

  /* These two attributes are emitted if all of the code is contiguous.  */
  if (all_segs->next == NULL)
    {
      /* DW_AT_low_pc */
      exp.X_op = O_symbol;
      exp.X_add_symbol = all_segs->text_start;
      exp.X_add_number = 0;
      emit_expr (&exp, sizeof_address);

      /* DW_AT_high_pc */
      if (DWARF2_VERSION < 4)
	exp.X_op = O_symbol;
      else
	{
	  exp.X_op = O_subtract;
	  exp.X_op_symbol = all_segs->text_start;
	}
      exp.X_add_symbol = all_segs->text_end;
      exp.X_add_number = 0;
      emit_expr (&exp, sizeof_address);
    }
  else
    {
      /* This attribute is emitted if the code is disjoint.  */
      /* DW_AT_ranges.  */
      TC_DWARF2_EMIT_OFFSET (section_symbol (ranges_seg), sizeof_offset);
    }

  /* DW_AT_name, DW_AT_comp_dir and DW_AT_producer.  Symbols in .debug_str
     setup in out_debug_str below.  */
  TC_DWARF2_EMIT_OFFSET (name_sym, sizeof_offset);
  TC_DWARF2_EMIT_OFFSET (comp_dir_sym, sizeof_offset);
  TC_DWARF2_EMIT_OFFSET (producer_sym, sizeof_offset);

  /* DW_AT_language.  Yes, this is probably not really MIPS, but the
     dwarf2 draft has no standard code for assembler.  */
  out_two (DW_LANG_Mips_Assembler);

  symbol_set_value_now (info_end);
}

/* Emit the three debug strings needed in .debug_str and setup symbols
   to them for use in out_debug_info.  */
static void
out_debug_str (segT str_seg, symbolS **name_sym, symbolS **comp_dir_sym,
	       symbolS **producer_sym)
{
  char producer[128];
  const char *comp_dir;
  const char *dirname;
  char *p;
  int len;

  subseg_set (str_seg, 0);

  /* DW_AT_name.  We don't have the actual file name that was present
     on the command line, so assume files[1] is the main input file.
     We're not supposed to get called unless at least one line number
     entry was emitted, so this should always be defined.  */
  *name_sym = symbol_temp_new_now ();
  if (files_in_use == 0)
    abort ();
  if (files[1].dir)
    {
      dirname = remap_debug_filename (dirs[files[1].dir]);
      len = strlen (dirname);
#ifdef TE_VMS
      /* Already has trailing slash.  */
      p = frag_more (len);
      memcpy (p, dirname, len);
#else
      p = frag_more (len + 1);
      memcpy (p, dirname, len);
      INSERT_DIR_SEPARATOR (p, len);
#endif
    }
  len = strlen (files[1].filename) + 1;
  p = frag_more (len);
  memcpy (p, files[1].filename, len);

  /* DW_AT_comp_dir */
  *comp_dir_sym = symbol_temp_new_now ();
  comp_dir = remap_debug_filename (getpwd ());
  len = strlen (comp_dir) + 1;
  p = frag_more (len);
  memcpy (p, comp_dir, len);

  /* DW_AT_producer */
  *producer_sym = symbol_temp_new_now ();
  sprintf (producer, "GNU AS %s", VERSION);
  len = strlen (producer) + 1;
  p = frag_more (len);
  memcpy (p, producer, len);
}

void
dwarf2_init (void)
{
  last_seg_ptr = &all_segs;
}


/* Finish the dwarf2 debug sections.  We emit .debug.line if there
   were any .file/.loc directives, or --gdwarf2 was given, or if the
   file has a non-empty .debug_info section and an empty .debug_line
   section.  If we emit .debug_line, and the .debug_info section is
   empty, we also emit .debug_info, .debug_aranges and .debug_abbrev.
   ALL_SEGS will be non-null if there were any .file/.loc directives,
   or --gdwarf2 was given and there were any located instructions
   emitted.  */

void
dwarf2_finish (void)
{
  segT line_seg;
  struct line_seg *s;
  segT info_seg;
  int emit_other_sections = 0;
  int empty_debug_line = 0;

  info_seg = bfd_get_section_by_name (stdoutput, ".debug_info");
  emit_other_sections = info_seg == NULL || !seg_not_empty_p (info_seg);

  line_seg = bfd_get_section_by_name (stdoutput, ".debug_line");
  empty_debug_line = line_seg == NULL || !seg_not_empty_p (line_seg);

  /* We can't construct a new debug_line section if we already have one.
     Give an error.  */
  if (all_segs && !empty_debug_line)
    as_fatal ("duplicate .debug_line sections");

  if ((!all_segs && emit_other_sections)
      || (!emit_other_sections && !empty_debug_line))
    /* If there is no line information and no non-empty .debug_info
       section, or if there is both a non-empty .debug_info and a non-empty
       .debug_line, then we do nothing.  */
    return;

  /* Calculate the size of an address for the target machine.  */
  sizeof_address = DWARF2_ADDR_SIZE (stdoutput);

  /* Create and switch to the line number section.  */
  line_seg = subseg_new (".debug_line", 0);
  bfd_set_section_flags (stdoutput, line_seg, SEC_READONLY | SEC_DEBUGGING);

  /* For each subsection, chain the debug entries together.  */
  for (s = all_segs; s; s = s->next)
    {
      struct line_subseg *lss = s->head;
      struct line_entry **ptail = lss->ptail;

      while ((lss = lss->next) != NULL)
	{
	  *ptail = lss->head;
	  ptail = lss->ptail;
	}
    }

  out_debug_line (line_seg);

  /* If this is assembler generated line info, and there is no
     debug_info already, we need .debug_info, .debug_abbrev and
     .debug_str sections as well.  */
  if (emit_other_sections)
    {
      segT abbrev_seg;
      segT aranges_seg;
      segT ranges_seg;
      segT str_seg;
      symbolS *name_sym, *comp_dir_sym, *producer_sym;

      gas_assert (all_segs);

      info_seg = subseg_new (".debug_info", 0);
      abbrev_seg = subseg_new (".debug_abbrev", 0);
      aranges_seg = subseg_new (".debug_aranges", 0);
      str_seg = subseg_new (".debug_str", 0);

      bfd_set_section_flags (stdoutput, info_seg,
			     SEC_READONLY | SEC_DEBUGGING);
      bfd_set_section_flags (stdoutput, abbrev_seg,
			     SEC_READONLY | SEC_DEBUGGING);
      bfd_set_section_flags (stdoutput, aranges_seg,
			     SEC_READONLY | SEC_DEBUGGING);
      bfd_set_section_flags (stdoutput, str_seg,
			     (SEC_READONLY | SEC_DEBUGGING
			      | SEC_MERGE | SEC_STRINGS));
      str_seg->entsize = 1;

      record_alignment (aranges_seg, ffs (2 * sizeof_address) - 1);

      if (all_segs->next == NULL)
	ranges_seg = NULL;
      else
	{
	  ranges_seg = subseg_new (".debug_ranges", 0);
	  bfd_set_section_flags (stdoutput, ranges_seg,
				 SEC_READONLY | SEC_DEBUGGING);
	  record_alignment (ranges_seg, ffs (2 * sizeof_address) - 1);
	  out_debug_ranges (ranges_seg);
	}

      out_debug_aranges (aranges_seg, info_seg);
      out_debug_abbrev (abbrev_seg, info_seg, line_seg);
      out_debug_str (str_seg, &name_sym, &comp_dir_sym, &producer_sym);
      out_debug_info (info_seg, abbrev_seg, line_seg, ranges_seg,
		      name_sym, comp_dir_sym, producer_sym);
    }
}

/* Perform any deferred checks pertaining to debug information.  */

void
dwarf2dbg_final_check (void)
{
  /* Perform reset-view checks.  Don't evaluate view_assert_failed
     recursively: it could be very deep.  It's a chain of adds, with
     each chain element pointing to the next in X_add_symbol, and
     holding the check value in X_op_symbol.  */
  while (view_assert_failed)
    {
      expressionS *exp;
      symbolS *sym;
      offsetT failed;

      gas_assert (!symbol_resolved_p (view_assert_failed));

      exp = symbol_get_value_expression (view_assert_failed);
      sym = view_assert_failed;

      /* If view_assert_failed looks like a compound check in the
	 chain, break it up.  */
      if (exp->X_op == O_add && exp->X_add_number == 0 && exp->X_unsigned)
	{
	  view_assert_failed = exp->X_add_symbol;
	  sym = exp->X_op_symbol;
	}
      else
	view_assert_failed = NULL;

      failed = resolve_symbol_value (sym);
      if (!symbol_resolved_p (sym) || failed)
	{
	  as_bad (_("view number mismatch"));
	  break;
	}
    }
}
