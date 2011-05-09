package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.Arrays;
import java.util.Comparator;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FileTree implements IsSerializable {

    private FileTree[] children = new FileTree[0];
    private String name;

    private String fullpath;
    private boolean magic_check;
    private boolean checked_child;

    public String getFullpath() {
        return fullpath;
    }

    public void setMagicCheck(boolean checked) {
        magic_check = checked;
    }

    public boolean getMagicCheck() {
        return magic_check;
    }

    public void setCheckedChild(boolean check) {
        checked_child = check;
    }

    public boolean getCheckedChild() {
        return checked_child;
    }

    public void setFullpath(String fullpath) {
        this.fullpath = fullpath;
    }

    public FileTree() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void print(int spaces) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            b.append(" ");
        }
        System.out.println(b + name);
        if (children != null) {
            for (FileTree child : children) {
                child.print(spaces + 1);
            }
        }
    }

    public void sortRecursive() {

        Arrays.sort(children, new Comparator<FileTree>() {
            public int compare(FileTree o1, FileTree o2) {
                if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                } else {
                    return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                }
            }
        });
        if (children != null) {
            for (FileTree child : children) {
                child.sortRecursive();
            }
        }
    }

    public FileTree[] getChildren() {
        return children;
    }

    public void setChildren(FileTree[] children) {
        this.children = children;
    }

    public boolean matches(String filter) {
        if (filter == null || filter.length() == 0) {
            return true;
        }
        if (name.toLowerCase().contains(filter.toLowerCase())) {
            return true;
        }
        if (children == null) {
            return false;
        }
        for (FileTree child : children) {
            if (child.matches(filter)) {
                return true;
            }
        }
        return false;
    }
}
