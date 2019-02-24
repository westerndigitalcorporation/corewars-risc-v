package il.co.codeguru.corewars_riscv.jsadd;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Dictionary;

import javax.lang.model.util.ElementScanner6;

class Entry {
    static Entry newDirEntry(String name) {
        Entry e = new Entry();
        e.isDir = true;
        e.name = name;
        e.children = new ArrayList<Entry>();
        return e;
    }
    public Entry addChild(String name, byte[] data) {
        Entry e = new Entry();
        e.isDir = false;
        e.name = name;
        e.data = data;
        children.add(e);
        return e;
    }

    String name;
    public boolean isDir = false;
    public byte[] data = null; // in case its a real file
    ArrayList<Entry> children = null; // in case it's a directory
}

class Bytes {
    public static byte[] to(String s) throws IOException {
        ArrayList<Integer> result = new ArrayList<Integer>();
        int v = 0, shift = 4;    
        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == ' ')
                continue;
            if (c >= '0' && c <= '9')
                v |= (c - '0') << shift;
            else if (c >= 'A' && c <= 'F')
                v |= (c - 'A' + 10) << shift;
            else if (c >= 'a' && c <= 'f')
                v |= (c - 'a' + 10) << shift;
            else
                throw new IOException("uexpected character in hex string");
            if (shift == 0) {
                result.add(v);
                v = 0;
            }
           // System.out.println("~~" + Integer.toHexString(v) + " " + Integer.toString(shift));
            shift = 4 - shift;
        }
        byte[] arr = new byte[result.size()];
        for(int i = 0; i < result.size(); ++i)
            arr[i] = (byte)(int)result.get(i);

        //System.out.println("~~~" + from(arr));
        return arr;
    }

    public static String from(byte[] b) { 
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < b.length; ++i) {
            if (i > 0)
                sb.append(' ');
            int v = b[i] & 0xff;
            String s = Format.hex(v, 2);
            if (v < 16)
                sb.append('0');
            sb.append(s);
        }
        return sb.toString();
    }
}

// manages the virtual file system in javascript
class FileMgr
{
    private static FileMgr m_ins = new FileMgr();
    public ArrayList<Entry> m_root = new ArrayList<Entry>();



    private FileMgr() {
        try {

            Entry s = Entry.newDirEntry("survivors");
            m_root.add(s);
            s.addChild("bimp1", Bytes.to("1E 07 97 83 C7 0C 89 FE 83 C6 0A FD 4F 4F A5 A5 A5 A5 A5 A5 47 47 FF E7"));
            s.addChild("bimp2", Bytes.to("1E 07 97 83 C7 0C 89 FE 83 C6 0A FD 4F 4F A5 A5 A5 A5 A5 A5 47 47 FF E7"));
            s.addChild("shootedA1", Bytes.to("1E 07 89 C7 B8 CC CC AB 83 C7 0B EB FA"));
            s.addChild("shootedA2", Bytes.to("1E 07 89 C7 B8 CC CC AB 83 C7 0B EB FA"));
            s.addChild("shooterB",  Bytes.to("1E 07 89 C7 B8 CC CC AB 83 C7 0B EB FA"));
            s.addChild("shooterC",  Bytes.to("1E 07 89 C7 B8 CC CC AB 83 C7 0B EB FA"));
            Entry z = Entry.newDirEntry("zombies");
            m_root.add(z);



        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static FileMgr instance() {
        return m_ins;
    }
    

}


public class File
{
    Entry m_entry = null;

    public File(String name) throws IOException {
        assignEntry(FileMgr.instance().m_root, name);
       // System.out("Open file " + name + " " + m_entry.toString());
    }
    public File(Entry entry) {
        m_entry = entry;
    }

    private void assignEntry(ArrayList<Entry> parent, String name) throws IOException {
        for(Entry e : parent)
            if (e.name == name) {
                m_entry = e;
                break;
            }
        if (m_entry == null)
            throw new IOException("File not found " + name);
    }

    public File[] listFiles() throws IOException {
        if (!m_entry.isDir)
            throw new IOException("Not a directory " + m_entry.name);
        File[] res = new File[m_entry.children.size()];
        int i = 0;
        for (Entry e : m_entry.children ) {
            res[i++] = new File(e);
        }
        return res;
    }

    public String getName()  {
        return m_entry.name;
    }
    public boolean isDirectory() {
        return m_entry.isDir;
    }

    public int length() throws IOException {
        if (m_entry.isDir)
            throw new IOException("Not a file " + m_entry.name);
        return m_entry.data.length;
    }

    public byte[] getData() throws IOException {
        if (m_entry.isDir)
            throw new IOException("Not a file " + m_entry.name);
        return m_entry.data;
    }
    

}

