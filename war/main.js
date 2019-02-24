"use strict";

var run_assembler = null, run_nasm = null, run_gas = null
var PAGE_SIZE = 512
var LAST_PART_INDEX = Math.trunc(65535/PAGE_SIZE)

var did_start = false

var selcount = 1


// Java code comes to get and clear this variable to get the stdout of the nasm run
//var g_outputText = null
//function printConsole(line) {
//    //console.log("*** " + line)
//    g_outputText += line + '\n'
//}
// new ver of emscripten moved this to pre-js

// this is some emscripten voodoo that re-initialized the process memory
// it works in conjunction with the sbrk saving and rewind in run_gas to reset the memory state in
// each activation of the command line tool.
// Notice that it undoes some memory allocation made on process initialization
// building envars seem to be the only thing there and we don't care about that

function reinitMem() {
    // re-parse the base64 non-zero start value data segment. This assumes we don't have a separate mem file
    var memoryInitializerBytes = tryParseAsDataURI(memoryInitializer);
    HEAPU32.fill(0);
    HEAPU8.set(memoryInitializerBytes, GLOBAL_BASE);
    HEAP32[DYNAMICTOP_PTR>>2] = DYNAMIC_BASE; // basic memory allocation need this (done in global scope on the startup of the process)
}


function start()
{
    if (did_start)
        return;
    console.log("start()");
    run_gas = Module.cwrap('run_gas', null, ['string', 'string'])

    asm_edit.addEventListener("keydown", fixhscroll)
    asm_edit.addEventListener("keydown", fixTabKey) // want to capture the press since we want to prevent the default action
    asm_edit.addEventListener("keyup", fixhscroll)
    asm_edit.addEventListener("keyup", fixIndent)  // indent is on key up since we want it after the \n

    populate_debug_area()
    did_start = true

    setup_breakpoints_savers()
}


var saved_selectionStart = -1 // need to be int for java to read
var saved_selectionEnd = -1
var saved_keydown = null

// save the text area selection start and end in order to find out how to move the breakpoints
function setup_breakpoints_savers()
{
    asm_edit.addEventListener("keydown", function(e) {
        saved_selectionStart = asm_edit.selectionStart
        saved_selectionEnd = asm_edit.selectionEnd
        saved_keydown = e.key
    })

    var mouseHandler = function(e) {
        saved_selectionStart = asm_edit.selectionStart
        saved_selectionEnd = asm_edit.selectionEnd
        saved_keydown = null
    }
    // do both up and down to be safe. not sure if both are actually needed
    asm_edit.addEventListener("mousedown", mouseHandler)
    asm_edit.addEventListener("mouseup", mouseHandler)
}


function populate_debug_area()
{
    console.log("start addr add")
    var c = document.createDocumentFragment();
    var currentPart = null
    for(var i = 0; i <= 0xffff; ++i) {

        if ((i % PAGE_SIZE) == 0) {
            var parti = i / PAGE_SIZE
            currentPart = document.createElement('div')
            currentPart.classList = "dpart"
            currentPart.id = "dpart" + parti
            currentPart.style.display = ""
            c.appendChild(currentPart)

            var placeholder = document.createElement('div')
            placeholder.classList = "dplace"
            placeholder.id = "dplace" + parti
            placeholder.style.display = "none"

            if (i != 0) { // show the first one so it could give its height to the rest
                currentPart.style.display = "none"
                placeholder.style.display = ""
            }
            c.appendChild(placeholder)
        }
        var addr = i.toString(16).padStart(4, '0')
        var e = document.createElement('div')
        e.classList = 'dline'
        e.id = 'd' + i
        e.innerText = addr

        currentPart.appendChild(e);
    }
    debug_text.appendChild(c)
    console.log("done addr add")
}

function cssRuleBySelector(selText) {
    var rules = document.styleSheets[0].cssRules
    for(var i = 0; i < rules.length; ++i)
        if (rules[i].selectorText == selText)
            return rules[i]
    return null
}

var dplace_css = null
var single_part_height = 0
function fix_debug_scroll_placeholders_size()
{
    if (dplace_css == null)
        dplace_css = cssRuleBySelector('.dplace')
    // part0 is the only one displayed at the start so it would give its height to the rest
    single_part_height = dpart0.offsetHeight
    if (single_part_height == 0)
        throw "did not get placeholder height"
    dplace_css.style.height = dpart0.offsetHeight + "px"
}

// 0 is the only one shown at the start
// this array is always size 2
var last_shown_parts = [0,-1]

function hidePage(i) {
    if (i == -1)
        return
    document.getElementById('dpart' + i).style.display = 'none'
    document.getElementById('dplace' + i).style.display = ''
}
function showPage(i) {
    if (i == -1)
        return
    document.getElementById('dpart' + i).style.display = ''
    document.getElementById('dplace' + i).style.display = 'none'
}

// need to maintain who's open and who's not
// at all time, exactly two parts are open
// force does the scroll painting even if we're already there, need this when starting debug to update dirty pages
function debugAreaScroll(force)
{
    var atScroll = debug_text.scrollTop
    var atpartf = atScroll / single_part_height
    var atpart = Math.trunc(atpartf)
    var where_in_part = atpartf % 1  // fraction part

    var new_shown_parts = [atpart]
    var second_part = (where_in_part > 0.5) ? (atpart + 1) : (atpart - 1)
    if (second_part >= 0 && second_part <= LAST_PART_INDEX) {
        if (second_part > atpart)
            new_shown_parts.push(second_part)
        else
            new_shown_parts = [second_part, atpart] // maintain the first one be the smallest one for the above comparison
    }
    else
        new_shown_parts.push(-1)

    // this will still work fine if the array has only 1 element, [1] in both will be undefined
    if (!force && new_shown_parts[0] == last_shown_parts[0] && new_shown_parts[1] == last_shown_parts[1])
        return

    hidePage(last_shown_parts[0])
    hidePage(last_shown_parts[1])

    showPage(new_shown_parts[0])
    showPage(new_shown_parts[1])

    j_setScrollAt(new_shown_parts[0], new_shown_parts[1]);

    last_shown_parts = new_shown_parts

}

function scrollToAddr(addr) {
    if (addr < 0 || addr > 0xffff)
        return
    var e = document.getElementById("d" + addr)
    if (!e)
        return
    var eoffs = e.offsetTop
    if (eoffs > debug_text.scrollTop && eoffs < debug_text.scrollTop + debug_text.offsetHeight - 30)
        return // already in view, don't need to scroll

    e.parentElement.style.display = ""  // need to show it for it to have an offsetTop
    e.style.display = "" // just in case it is hidden for some reason
    debug_text.scrollTop = e.offsetTop - 30  // have some margin from the top
}

function triggerGoTo() {
    var addr = parseInt(debug_goto.value, 16)
    if (isNaN(addr)) {
        console.error("failed to parse address");
        return
    }
    if (addr < 0  || addr > 0xffff) {
        console.error("address out of range");
    }
    scrollToAddr(addr)
}
function triggerGoToKey() {
    if(event.key !== "Enter")
        return;
    event.preventDefault(); // No need to `return false;`.
    triggerGoTo()
}





// called when GWT finshed loading
function gwtStart()
{
    start();
    console.log("gwtStart()");
   // start with two players
    addPlayersPanel()
    addPlayersPanel()

    sel_code_w1_pA.checked = true
    triggerSrc('pA', 1)

    j_demoDebugPlayers();

    addWatchLine();
    //addWatchLine();
    //addWatchLine();
}


function addTextChild(elem, txt) {
    var dummy = document.createElement("DIV")
    dummy.innerHTML = txt
    elem.appendChild(dummy.firstChild)
}

var g_nextLetter = 'A'
var g_usedLetters = []
var g_srcButtons = []  // ids of elements that are checkboxes that select a source file to be displayer
var g_usedZnums = []

function asciiAdd(letter, val) {
    return String.fromCharCode(letter.charCodeAt(0) + val)
}

function arrayRemove(arr, value) {
    var index = arr.indexOf(value);
    console.assert(index > -1, "did not find " + value)
    arr.splice(index, 1);
}


function addPlayersPanel() {
    if (addPlayerBtn.getAttribute("disabled") == "true")
        return;
    if (g_usedLetters.length >= 20)
        return; // max players

    var text = '<div id="pl_frame_pLETTER" class="toppic">\
          <div class="famtitle">\
            <input class="fam_check_box hidden mycheck" id="player_check_pLETTER" type="checkbox" checked onchange="" >\
            <label class="fam_check mycheck_label" for="player_check_pLETTER"></label>\
            <label id="player_name_lbl_pLETTER" class="fam_label">Player LETTER</label>\
            <label id="player_erase_pLETTER" class="pl_close_icon" onclick="triggerErasePlayer(this, pl_frame_pLETTER, \'LETTER\')"></label>\
          </div>\
          <input class="fam_check_box hidden mycheck" id="wtype_pLETTER" type="checkbox" onchange="changedWType(\'pLETTER\', this.checked)" >\
          <label class="fam_check mycheck_label wtype_label" for="wtype_pLETTER">Two Warriors</label>\
          <input id="sel_code_w1_pLETTER" class="hidden sc-check" onclick="triggerSrc(\'pLETTER\', 1)" type="radio" name="src_select">\
          <label id="sel_code_lbl_w1_pLETTER" class="sc-btn src_sel_but" for="sel_code_w1_pLETTER" >Player_LETTER</label>\
          <br>\
          <input id="sel_code_w2_pLETTER" class="hidden sc-check" onclick="triggerSrc(\'pLETTER\', 2)" type="radio" name="src_select">\
          <label id="sel_code_lbl_w2_pLETTER" class="sc-btn src_sel_but" for="sel_code_w2_pLETTER">Player_LETTER2</label>\
        </div>'

    addTextChild(players_contaier, text.replace(/LETTER/g, g_nextLetter))

    var letter = g_nextLetter
    g_srcButtons.push('sel_code_w1_p' + letter)
    g_srcButtons.push('sel_code_w2_p' + letter)

    j_addPlayer('p'+letter, "Player " + letter)
    changedWType('p'+letter, false) // do update label

    g_usedLetters.push(letter)
    g_nextLetter = asciiAdd(g_nextLetter, 1)


}

function changedWType(label, v, move_ui)
{
    if (v === true)
        v = 'TWO_DIFFERENT'
    else if (v == false)
        v = 'SINGLE'

    if (move_ui) {
        var checked = (v = 'TWO_DIFFERENT')
        document.getElementById('wtype_' + label).checked = checked
    }

    var elem1 = document.getElementById('sel_code_lbl_w1_' + label)
    var elem2 = document.getElementById('sel_code_lbl_w2_' + label)
    var frame = document.getElementById('pl_frame_' + label)

    if (v == 'SINGLE' || v == 'TWO_IDENTICAL') {
        elem2.style.visibility = 'hidden'
        elem2.style.opacity = 0.0
        elem1.style.marginTop = '7px'
        frame.style.height = '98px'
    }
    else if (v == 'TWO_DIFFERENT') {
        elem2.style.visibility = 'visible'
        elem2.style.opacity = 1
        elem1.style.marginTop = '7px'
        frame.style.height = ''
    }

    j_changedWType(label, v);

}


function triggerSrc(label, index)
{
    j_srcSelectionChanged(label, index)
}

function triggerErasePlayer(buttonElem, elem, letter)
{
    if (buttonElem.getAttribute("disabled") == "true")
        return;
    //elem.parentNode.removeChild(elem);
    elem.addEventListener('animationend', function() {
        elem.parentNode.removeChild(elem);
    })
    elem.classList.add('removed_anim')

    arrayRemove(g_usedLetters, letter);
    arrayRemove(g_srcButtons, 'sel_code_w1_p' + letter)
    arrayRemove(g_srcButtons, 'sel_code_w2_p' + letter)

    j_removePlayer('p'+letter)

    if (g_usedLetters.length == 0) {
        g_nextLetter = 'A'
    }
    else
    {
        // figure out what would be the next letter
        var check = 'Z' // check from Z backwards
        while(true) {
            if (g_usedLetters.indexOf(check) > -1)
                break;
            check = asciiAdd(check, -1)
        }
        if (check != 'Z')
            g_nextLetter = asciiAdd(check, 1)
        else {  // all letters are used up, check for holes
            check = 'A'
            while(check != 'Z') {
                if (g_usedLetters.indexOf(check) == -1)
                    break;
                check = asciiAdd(check, 1)
            }
            g_nextLetter = check // upper limit on number of players should avoid this causing a problem
        }
    }
}

var g_nextZombNum = 1

function addZombieCode() {
    if (addZombieBtn.getAttribute("disabled") == "true")
        return;
    var text = '<div id="zomb_line_zNUM">\
        <input id="sel_code_w1_zNUM" class="hidden sc-check" onclick="triggerSrc(\'zNUM\', 1)" type="radio" name="src_select">\
        <label id="player_name_lbl_zNUM" class="sc-btn src_sel_but zomb_sel_but" for="sel_code_w1_zNUM" >Zombie NUM</label>\
        <label id="player_erase_zNUM" class="pl_close_icon za_close_icon" onclick="triggerEraseZombie(this, zomb_line_zNUM, \'NUM\')"></label>\
        </div>'

    addTextChild(zombies_container, text.replace(/NUM/g, g_nextZombNum))
    var num = '' + g_nextZombNum  // turn to a string
    g_nextZombNum += 1

    g_usedZnums.push(num)

    j_addPlayer('z'+num, "Zombie " + num) // players are identified by a letter, zombies by a number

}

function triggerEraseZombie(buttonElem, elem, num) {
    if (buttonElem.getAttribute("disabled") == "true")
        return;

    elem.parentNode.removeChild(elem);
    arrayRemove(g_usedZnums, num)

    j_removePlayer('z'+num)
}




// in case there is a long line, when returning to a new line, the text will be still scrolled to the left, fix that
function fixhscroll(e) {
    if (e.target != asm_edit)
        return;

    if (asm_edit.selectionStart == 0 || asm_edit.selectionStart == 1)
    {
        // console.log("fixing scroll")
        asm_edit.scrollLeft = 0
        asm_edit.scrollTop = 0
    }
    else if (asm_edit.value[asm_edit.selectionStart-1] == '\n' || asm_edit.value[asm_edit.selectionStart-2] == '\n')
    {
        asm_edit.scrollLeft = 0
    }
}



// called when a warning or error line in the editor is double clicked
function asm_cursorToLine(charOffset) {
    asm_edit.selectionStart = charOffset
    asm_edit.selectionEnd = charOffset
    asm_edit.focus()

}

/*function asm_pre_mousewheel(event) {
    asm_edit.dispatchEvent(new event.constructor(event.type, event));
}  scrolling in the code and numbers panes not working */


function handle_asm_edit_scroll() {
    var roundedScroll = Math.floor(asm_edit.scrollTop);

    // Fixes issue of desync text on mouse wheel, fuck Firefox.
    if(navigator.userAgent.toLowerCase().indexOf('firefox') < 0) {
        asm_edit.scrollTop = roundedScroll;
    }

    asm_pre.style.top = "-" + roundedScroll + "px"
    //asm_pre.scrollTop = asm_edit.scrollTop

    asm_show.style.left = "-" + asm_edit.scrollLeft + "px"
}

function fixTabKey(e) {
    if (e.keyCode === 9){
        var selStartPos = asm_edit.selectionStart
        var inputVal = asm_edit.value;
        e.preventDefault();

        asm_edit.value = inputVal.substring(0, selStartPos) + "    " + inputVal.substring(selStartPos, inputVal.length);
        asm_edit.selectionStart = selStartPos + 4;
        asm_edit.selectionEnd = selStartPos + 4;
        asm_edit.dispatchEvent(new Event("input")) // trigger the java parsing
    }
}

function fixIndent(e) {
    if (e.keyCode === 13)
    { // line indentation
        var selStartPos = asm_edit.selectionStart
        var inputVal = asm_edit.value;
        var pos = selStartPos - 2 // skip the last added \n (previous line)
        var seenSpaces = 0;
        // go to the start of the line, check if there is indentation there
        while (pos >= 0) {
            if (inputVal[pos] == ' ')
                ++seenSpaces;
            else if (inputVal[pos] == '\n')
                break;
            else
                seenSpaces = 0;
            --pos;
        }
        if (seenSpaces > 0) {
            // problem with this is it flushes the undo buffer, test initTextEvent ?
            asm_edit.value = inputVal.substring(0, selStartPos) + " ".repeat(seenSpaces) + inputVal.substring(selStartPos, inputVal.length);
            asm_edit.selectionStart = selStartPos + seenSpaces;
            asm_edit.selectionEnd = selStartPos + seenSpaces;
            j_asm_edit_changed() // simulate input
        }
    }
}

var debug_area_shown = false
var deferredEditorToAddress = -1 // set by code in CodeEditor.java

var prevDebug = false;
function triggerDebug() {
    console.log("triggerDebug " + debugCheckbox.checked)
    if (prevDebug == debugCheckbox.checked) // not sure how this is possible
        return;
    prevDebug = debugCheckbox.checked

    if (debugCheckbox.checked)
    {
        if (!j_startDebug()) {
            console.error("cannot start debug")
            debugCheckbox.checked = false;
            prevDebug = debugCheckbox.checked
            return
        }

        debugBtnIn.style.backgroundImage = "url(red_stop.png)"
        debugBtnIn2.innerText = "Stop"
        competeCheckbox.setAttribute("disabled", true);

        edit_area.style.display="none"
        debug_area.style.display=""
        if (!debug_area_shown) {
            fix_debug_scroll_placeholders_size()
            debug_area_shown = true; // need to do this fix only on the first time it's shown
        }

        // we do this only here because it needs to be done after the current page is shown.
        if (deferredEditorToAddress == -1)
            throw "no deferred scroll set?"

        scrollToAddr(deferredEditorToAddress)
        deferredEditorToAddress = -1;
        debugAreaScroll(true)  // force scroll render due to dirty pages
        j_debugUiInited();

        // disable editing the players
        addPlayerBtn.setAttribute("disabled", true)
        for(var ui in g_usedLetters) {
            var u = g_usedLetters[ui]
            document.getElementById("player_erase_p" + u).setAttribute("disabled", true)
            document.getElementById("player_check_p" + u).setAttribute("disabled", true)
            document.getElementById("wtype_p" + u).setAttribute("disabled", true)
        }
        player_check_zombies.setAttribute("disabled", true)
        addZombieBtn.setAttribute("disabled", true)
        for(var uzi in g_usedZnums) {
            var uz = g_usedZnums[uzi]
            document.getElementById("player_erase_z" + uz).setAttribute("disabled", true)
        }
        $('#settingsBtn').addClass('hide');
    }
    else {
        j_stopDebug()

        debugBtnIn.style.backgroundImage = ""
        debugBtnIn2.innerText = "Debug"
        competeCheckbox.removeAttribute("disabled");

        edit_area.style.display=""
        debug_area.style.display="none"


        addPlayerBtn.removeAttribute("disabled")
        for(var ui in g_usedLetters) {
            var u = g_usedLetters[ui]
            document.getElementById("player_erase_p" + u).removeAttribute("disabled")
            document.getElementById("player_check_p" + u).removeAttribute("disabled")
            document.getElementById("wtype_p" + u).removeAttribute("disabled")
        }
        player_check_zombies.removeAttribute("disabled")
        addZombieBtn.removeAttribute("disabled")
        for(var uzi in g_usedZnums) {
            var uz = g_usedZnums[uzi]
            document.getElementById("player_erase_z" + uz).removeAttribute("disabled")
        }
        $('#settingsBtn').removeClass('hide');
    }
}

function triggerRegBase(ishex) {
    if (ishex) {
        hexCheckbox.checked = true;
        decCheckbox.checked = false;
    }
    else {
        hexCheckbox.checked = false;
        decCheckbox.checked = true;
    }
    j_setRegistersBase(ishex ? 16:10)
}

function loadAddrUpdateUI() {
    if (asm_load_addr_type.value == "rand") {
        fixed_load_addr.style.visibility = "hidden";
        fixed_load_addr.style.opacity = "0.0";
    }
    else {
        fixed_load_addr.style.visibility = "visible";
        fixed_load_addr.style.opacity = "1.0";
    }

}
function changedLoadAddrType() {
    loadAddrUpdateUI()
    j_loadAddrChanged(fixed_load_addr.value, asm_load_addr_type.value == "rand");

}
function changedFixedAddr() {
    j_loadAddrChanged(fixed_load_addr.value, asm_load_addr_type.value == "rand");
}
// from java, when changing warrior
function updateLoadAddr(strValue, isRand) {
    asm_load_addr_type.value = isRand ? "rand":"fixed"
    fixed_load_addr.value = strValue
    loadAddrUpdateUI()
}


function addTableChild(elem, txt) {
    var dummy = document.createElement("TABLE")
    dummy.innerHTML = txt
    elem.appendChild(dummy.firstChild.firstChild)
    // firstChild of table is tbody added automatically, bypass it
}

// grip idea from http://jsfiddle.net/thrilleratplay/epcybL4v/
var g_watchId = 0;
var watch_expr_list = []
var watch_expr_size = null  // size as dragged by the grip

function addWatchLine() {
    var wid = g_watchId++

    var text = '<div class="watch_row" id="watchID">\
                  <div class="watch_expr allow_select" id="watchID_expr" style="width:100px;">\
                     <div id="watchID_expr_content" class="watch_expr_content">&nbsp;</div>\
                     <input type="text" class="watch_expr_edit" id="watchID_expr_edit" spellcheck="false">\
                     <div class="watch_grip" id="watchID_grip"></div>\
                  </div><div class="watch_val allow_select" id="watchID_val"></div>\
                  <div id="watchID_del_but" class="sc-btn watch_del_but" title="Remove watch"></div> \
               </div>'
    text = text.replace(/ID/g, wid)

    addTextChild(watchArea, text)
    //add_watch_but.style.top = (watch_expr_list.length * 21 + 21) + "px"

    var grip = document.getElementById("watchID_grip".replace(/ID/g, wid) )
    var expr = document.getElementById("watchID_expr".replace(/ID/g, wid) )
    watch_expr_list.push(expr)
    if (watch_expr_size != null)
        expr.style.width = watch_expr_size + 'px';
    var expr_edit = document.getElementById("watchID_expr_edit".replace(/ID/g, wid) )
    var expr_content = document.getElementById("watchID_expr_content".replace(/ID/g, wid) )
    var valu = document.getElementById("watchID_val".replace(/ID/g, wid) )
    var delbut = document.getElementById("watchID_del_but".replace(/ID/g, wid) )

    j_addWatch(wid)

    expr.addEventListener('click', function(e) {
        expr_edit.style.visibility = "visible"
        expr_edit.focus()
        // TBD should somehow put the cursor in the expected place inside expr_edit
        // measure the text and use selectionStart, selectionEnd to place the cursor

    });
    expr_edit.addEventListener('change', function() { // enter was pressed
        //expr_content.innerText = expr_edit.value;
        expr_edit.style.visibility = "hidden"
        // if it's the last one being edited and there is a non empty value written in it, add another one
        if (watch_expr_list.indexOf(expr) == watch_expr_list.length - 1 && valu.innerText != "") {
            addWatchLine();
        }
    });
    expr_edit.addEventListener('input', function() { // enter was pressed
        var result = j_watchTextChanged(expr_edit.value, wid)
        if (result == false)
            expr_content.innerHTML = "&nbsp;"; // without this the watch window layout gets messed up for some reason
        else
            expr_content.innerText = expr_edit.value;
    });
    expr_edit.addEventListener('blur', function() { // enter was pressed
        expr_edit.style.visibility = "hidden"
    });

    delbut.addEventListener("click", function() {
        if (watch_expr_list.length == 1)
            return // don't delete the last one
        if (watch_expr_list.indexOf(expr) == watch_expr_list.length - 1 && valu.innerText == "")
            return // don't delete the last one that is empty that was last edded
        var idx = watch_expr_list.indexOf(expr)
        if (idx == -1)
            return // should not happen
        var row = document.getElementById("watchID".replace(/ID/g, wid) )
        watchArea.removeChild(row)
        j_delWatch(wid)
        watch_expr_list.splice(idx, 1)
    });


    var moving = false;
    var startOffset;

    grip.addEventListener('mousedown', function(e) {
        moving = true;
        startOffset = watch_expr_list[0].offsetWidth - e.pageX;
        //consolelog("CAPTURE")
    });


    document.addEventListener('mousemove', function(e) {
      if (moving) {
          watch_expr_size = startOffset + e.pageX
          for(var elemi in watch_expr_list)
              watch_expr_list[elemi].style.width = watch_expr_size + 'px';
          e.preventDefault(); // prevent selection action from messing it up

          if (watch_expr_size > watchArea.offsetWidth - 50)
              watchArea.style.overflowX = "auto"
          else
              watchArea.style.overflowX = "hidden"
      }
    });

    document.addEventListener('mouseup', function() {
        moving = false;
    });

}

function triggerUploadBinChanged()
{
    if (uploadBinInput.files.length == 0)
        return
    var file = uploadBinInput.files[0]
    uploadBinInput.value = ""  // reset it so that next time we could upload the same filename again

    var reader = new FileReader();
    reader.onload = function(e) {
        j_loadBinary(e.target.result)
    }
    reader.onerror = function(e) {
        console.error(e)
    }
    reader.readAsArrayBuffer(file);


    return false
}

//-------------------------------- warCanvas wheel zoom -----------------
// https://github.com/jackmoore/wheelzoom/blob/master/wheelzoom.js

var width;
var height;
var bgWidth;
var bgHeight;
var bgPosX;
var bgPosY;
var previousEvent = null;
var downEvent = null
var settings = {
    zoom: 0.10,
    maxZoom: false,
    initialZoom: 1,
};
var warCtx = null;

function js_updateFromKeyScroll(nx, ny) {
    bgPosX = nx;
    bgPosY = ny;
    updateBgVars();
}

function updateBgVars() {
    if (bgPosX > 0) {
        bgPosX = 0;
    } else if (bgPosX < width - bgWidth) {
        bgPosX = width - bgWidth;
    }

    if (bgPosY > 0) {
        bgPosY = 0;
    } else if (bgPosY < height - bgHeight) {
        bgPosY = height - bgHeight;
    }

    var hscale = bgWidth / width;
    var vscale = bgHeight / height
    j_warCanvas_setTransform(hscale, vscale, bgPosX, bgPosY);
}

function updateBgStyle() {
    updateBgVars();
    j_warCanvas_repaint();
}

function wheelzoom_reset() {
    bgWidth = width;
    bgHeight = height;
    bgPosX = bgPosY = 0;
    updateBgStyle();
}

function onWarCanvasWheel(e) {
    var deltaY = 0;

    e.preventDefault();

    if (e.deltaY) { // FireFox 17+ (IE9+, Chrome 31+?)
        deltaY = e.deltaY;
    } else if (e.wheelDelta) {
        deltaY = -e.wheelDelta;
    }

    // As far as I know, there is no good cross-browser way to get the cursor position relative to the event target.
    // We have to calculate the target element's position relative to the document, and subtrack that from the
    // cursor's position relative to the document.
    var rect = warCanvas.getBoundingClientRect();
    var offsetX = e.pageX - rect.left - window.pageXOffset - WC_MARGIN_LEFT;
    var offsetY = e.pageY - rect.top - window.pageYOffset - WC_MARGIN_TOP;

    // Record the offset between the bg edge and cursor:
    var bgCursorX = offsetX - bgPosX;
    var bgCursorY = offsetY - bgPosY;

    // Use the previous offset to get the percent offset between the bg edge and cursor:
    var bgRatioX = bgCursorX/bgWidth;
    var bgRatioY = bgCursorY/bgHeight;

    // Update the bg size:
    if (deltaY < 0) {
        bgWidth += bgWidth*settings.zoom;
        bgHeight += bgHeight*settings.zoom;
    } else {
        bgWidth -= bgWidth*settings.zoom;
        bgHeight -= bgHeight*settings.zoom;
    }
    //console.log("sz: " + bgWidth + "," + bgHeight);

    if (settings.maxZoom) {
        bgWidth = Math.min(width*settings.maxZoom, bgWidth);
        bgHeight = Math.min(height*settings.maxZoom, bgHeight);
    }

    // Take the percent offset and apply it to the new size:
    bgPosX = offsetX - (bgWidth * bgRatioX);
    bgPosY = offsetY - (bgHeight * bgRatioY);

    // Prevent zooming out beyond the starting size
    if (bgWidth <= width || bgHeight <= height) {
        wheelzoom_reset();
    } else {
        updateBgStyle();
    }
}

function drag(e) {
    e.preventDefault();
    bgPosX += e.pageX - previousEvent.pageX;
    bgPosY += e.pageY - previousEvent.pageY;
    previousEvent = e;
    updateBgStyle();
}

function removeDrag(e) {
    var clickDx = Math.abs(e.pageX - downEvent.pageX);
    var clickDy = Math.abs(e.pageY - downEvent.pageY);
    if (clickDx < 6 && clickDy < 6) {
        var rect = warCanvas.getBoundingClientRect();
        var offsetX = e.pageX - rect.left - window.pageXOffset - WC_MARGIN_LEFT;
        var offsetY = e.pageY - rect.top - window.pageYOffset - WC_MARGIN_TOP;
        j_warCanvas_click(offsetX, offsetY);
    }


    document.removeEventListener('mouseup', removeDrag);
    document.removeEventListener('mousemove', drag);
}

function mouseMoveShowCurrent(e) {
    var rect = warCanvas.getBoundingClientRect();
    var offsetX = e.pageX - rect.left - window.pageXOffset - WC_MARGIN_LEFT;
    var offsetY = e.pageY - rect.top - window.pageYOffset - WC_MARGIN_TOP;
    j_warCanvas_showCurrent(offsetX, offsetY);
}

function warCanvasDraggable(e) {
    e.preventDefault();
    previousEvent = e;
    downEvent = e;
    document.addEventListener('mousemove', drag);
    document.addEventListener('mouseup', removeDrag);
}

function js_initZoom() {
    warCanvas.addEventListener('wheel', onWarCanvasWheel);
    warCanvas.addEventListener('mousedown', warCanvasDraggable);
    warCanvas.addEventListener('mousemove', mouseMoveShowCurrent);

    warCtx = warCanvas.getContext('2d');
}
function js_resetZoom() {
    var initial = 1;
    //var computedStyle = window.getComputedStyle(warCanvas, null);

    width = 256*3; //parseInt(computedStyle.width, 10);
    height = 256*3; //parseInt(computedStyle.height, 10);
    bgWidth = width * initial;
    bgHeight = height * initial;
    bgPosX = -(bgWidth - width)/2;
    bgPosY = -(bgHeight - height)/2;
    updateBgStyle();
}

// ------------------------------------------ Competition --------------------------------------------

function graph_panel_anim_end() {
    if (competeCheckbox.checked) {
        // setting the style in the normal way doesn't work here for some reason
        graphs_panel.setAttribute("style", "opacity: 1; transition: width 0.3s cubic-bezier(0.36, 0.29, 0.79, 1.07), opacity 0.3s cubic-bezier(0.56, 0.02, 0.89, 0.26);");
    }
    else {
        graphs_panel.style.transition = "";
    }
}

function gray_out_anim_end() {
    if (competeCheckbox.checked) {
    }
    else {
        gray_out_debugger.style.visibility = "hidden";
        //gray_out_debugger.style.transition = "";
    }
}

function openCompete() {
    if (competeCheckbox.checked) {
        graphs_panel.style.width = "";
        graphs_panel.style.opacity = 1;
        gray_out_debugger.style.visibility = "visible";
        gray_out_debugger.style.opacity = 1;
        debugCheckbox.setAttribute("disabled", true);
        debugBtn.setAttribute("disabled", true);
    }
    else {
        graphs_panel.style.width = "0";
        graphs_panel.style.opacity = 0;
        gray_out_debugger.style.opacity = 0;
        debugCheckbox.removeAttribute("disabled");
        debugBtn.removeAttribute("disabled");
    }
}

function triggerStartCompete()
 {
    if (competeRunCheckbox.checked)
    {
        if (!j_startCompete()) {
            console.error("cannot start competition")
            competeRunCheckbox.checked = false;
            return
        }

        competeRunBtnIn.style.backgroundImage = "url(red_stop.png)"
        competeRunBtnIn2.innerText = "Stop"
    }
    else {
        j_stopCompete()

    }
}
function competeFinished()
{
    competeRunBtnIn.style.backgroundImage = ""
    competeRunBtnIn2.innerText = "Start!"
    competeRunCheckbox.checked = false
}

function eventStopProp(e) {
    e.stopPropagation()
}
function hidePopup(event) {
    if($(event.target).closest('.popupWin').length == 0)
        window.location.replace('#');
}
