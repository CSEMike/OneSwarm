package org.gudy.azureus2.ui.swt.test;

import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * Application to identify supported URL drag and drop IDs from different
 * browsers.
 * 
 * @see org.gudy.azureus2.ui.swt.URLTransfer
 * @author Rene Leonhardt
 */
public class PrintTransferTypes extends ByteArrayTransfer {

  private static PrintTransferTypes _instance = new PrintTransferTypes();
  private int[] ids;
  private String[] names;

  public static void main(String[] args) {
    Display display = new Display();
    Shell shell = new Shell(display);
    shell.setLayout(new FillLayout());
    Canvas canvas = new Canvas(shell, SWT.NONE);
    DropTarget target = new DropTarget(canvas, DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_MOVE | DND.DROP_TARGET_MOVE | DND.DROP_NONE);
    target.setTransfer(new Transfer[] { PrintTransferTypes.getInstance(), TextTransfer.getInstance(), FileTransfer.getInstance()});
    target.addDropListener(new DropTargetAdapter() {
      public void dragEnter(DropTargetEvent event) {
        //        if(event.detail == DND.DROP_NONE)
        event.detail = DND.DROP_LINK;
        String ops = "";
        if ((event.operations & DND.DROP_COPY) != 0)
          ops += "Copy;";
        if ((event.operations & DND.DROP_MOVE) != 0)
          ops += "Move;";
        if ((event.operations & DND.DROP_LINK) != 0)
          ops += "Link;";
        System.out.println("Allowed Operations are " + ops);

        TransferData[] data = event.dataTypes;
        for (int i = 0; i < data.length; i++) {
          int id = data[i].type;
          String name = getNameFromId(id);
          System.out.println("Data type is " + id + " " + name);
        }
      }
      public void dragOver(DropTargetEvent event) {
        event.detail = DND.DROP_LINK;
      }
      public void drop(DropTargetEvent event) {
        System.out.println("URL dropped: " + event.data);
        System.out.println("Data type is " + event.currentDataType.type + " " + getNameFromId(event.currentDataType.type));
      }
    });

    shell.setSize(400, 400);
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

  public static PrintTransferTypes getInstance() {
    return _instance;
  }
  PrintTransferTypes() {
    ids = new int[50000];
    names = new String[50000];
    for (int i = 0; i < ids.length; i++) {
      ids[i] = i;
      names[i] = getNameFromId(i);
    }
  }
  public void javaToNative(Object object, TransferData transferData) {}
  public Object nativeToJava(TransferData transferData) {
    byte[] buffer = (byte[]) super.nativeToJava(transferData);
    if (buffer == null)
      return null;
    int size = buffer.length;
    byte[] text = new byte[size];
    int j = 0;
    for (int i = 0; i < buffer.length; i++) {
      if (buffer[i] != 0)
        text[j++] = buffer[i];
    }
    String data = new String(text, 0, j);
    int end = data.indexOf("\n");
    return end >= 0 ? data.substring(0, end) : data;
  }
  protected String[] getTypeNames() {
    return names;
  }
  protected int[] getTypeIds() {
    return ids;
  }
  static String getNameFromId(int id) {
    switch (id) {
      case 1 :
        return "CF_TEXT";
      case 8 :
        return "CF_DIB";
      case 13 :
        return "CF_UNICODETEXT";
      case 15 :
        return "CF_HDROP";
      case 49158 :
        return "FileName";
      case 49159 :
        return "FileNameW";
      case 49267 :
        return "Shell IDList Array";
      case 49350 :
        return "FileContents";
      case 49351 :
        return "FileGroupDescriptor";
      case 49352 :
        return "FileGroupDescriptorW";
      case 49356 :
        return "HTML Format";
      case 49357 :
        return "Preferred DropEffect";
      case 49361 :
        return "UniformResourceLocator";
      case 49362 :
        return "UniformResourceLocator"; // or InShellDragLoop
      case 49368 :
        return "UniformResourceLocator";
      case 49429 :
        return "UniformResourceLocatorW";
      case 49458 :
        return "UniformResourceLocatorW";
      case 49569 :
        return "text/html";
      case 49570 :
        return "text/_moz_htmlcontext";
      case 49571 :
        return "text/_moz_htmlinfo";
      case 49624 :
        return "application/x-moz-nativeimage";

    }
    return "*UNKNOWN_TYPE*";
  }
}