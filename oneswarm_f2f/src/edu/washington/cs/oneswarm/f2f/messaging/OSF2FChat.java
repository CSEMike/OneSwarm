package edu.washington.cs.oneswarm.f2f.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.lang.Entities;
import org.apache.ecs.xml.XML;
import org.apache.ecs.xml.XMLDocument;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FChat implements OSF2FMessage {

    public static final String MESSAGE_ELEMENT = "message";
    public static final String PLAIN_TEXT_ATTRIB = "plaintext";

    private static final int MAX_LENGTH = 1024 * 2; // 2k of XML ought to be
                                                    // enough for anybody

    private byte version;
    private String plain_text_message = null;
    private String description;
    private DirectByteBuffer buffer = null;
    private int mMessageSize = -1;

    public OSF2FChat(byte _version, String plain_text_message) {
        this.version = _version;

        if (plain_text_message != null) {
            if (plain_text_message.length() > MAX_LENGTH) {
                plain_text_message = plain_text_message.substring(0, MAX_LENGTH);
            }
        }

        this.plain_text_message = plain_text_message;
    }

    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        int length = data.remaining(DirectByteBuffer.SS_MSG);

        if (length > MAX_LENGTH) {
            throw new MessageException("[" + getID()
                    + "] decode error: message length greater than " + MAX_LENGTH + " (" + length
                    + ")");
        }

        byte[] in = new byte[length];
        data.get(DirectByteBuffer.SS_MSG, in);
        try {
            // to change: org.xml.sax.driver system property
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            DefaultHandler handler = new DefaultHandler() {
                public void startElement(String uri, String localName, String qName,
                        Attributes attributes) throws SAXException {
                    if (qName.equals(MESSAGE_ELEMENT)) {
                        plain_text_message = Entities.XML.unescape(attributes
                                .getValue(PLAIN_TEXT_ATTRIB));
                    }
                }
            };
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(new ByteArrayInputStream(in)));

            if (plain_text_message == null) {
                throw new MessageException("[" + getID()
                        + "] decode error: no plain text message after XML parsing");
            }

        } catch (SAXException e) {
            throw new MessageException("[" + getID() + "] XML decode error: "
                    + e.getClass().getName() + " / " + e.toString() + " bytes: " + (new String(in)));
        } catch (IOException e) {
            throw new MessageException("[" + getID() + "] XML decode error: "
                    + e.getClass().getName() + " / " + e.toString() + " bytes: " + (new String(in)));
        }

        data.returnToPool();
        return new OSF2FChat(version, plain_text_message);
    }

    public String getID() {
        return OSF2FMessage.ID_OS_CHAT;
    }

    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_CHAT_BYTES;
    }

    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_CHAT;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public byte getVersion() {
        return version;
    };

    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_CHAT + "\tchat=" + plain_text_message;
        }

        return description;
    }

    public void destroy() {
        if (buffer != null) {
            buffer.returnToPool();
            buffer = null;
        }
    }

    public DirectByteBuffer[] getData() {
        if (buffer == null) {

            XMLDocument doc = new XMLDocument();
            doc.addElement(new XML(MESSAGE_ELEMENT).addXMLAttribute(PLAIN_TEXT_ATTRIB,
                    org.apache.commons.lang.Entities.XML.escape(plain_text_message)));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.output(baos);

            byte[] output = baos.toByteArray();
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, output.length + 4);
            buffer.put(DirectByteBuffer.SS_MSG, output);
            buffer.flip(DirectByteBuffer.SS_MSG);

            mMessageSize = output.length;
        }

        return new DirectByteBuffer[] { buffer };
    }

    public String getPlainText() {
        return plain_text_message;
    }

    public int getMessageSize() {
        if (mMessageSize == -1) {
            getData();
        }
        return mMessageSize;
    }

    public static final void main(String[] args) throws Exception {
        OSF2FChat test = new OSF2FChat((byte) 1, "I sure like being inside this fancy computer.");
        OSF2FChat parse = new OSF2FChat((byte) 1, null);
        OSF2FChat test2 = (OSF2FChat) parse.deserialize(test.getData()[0], (byte) 1);

        System.out.println(test.getPlainText() + " / " + test2.getPlainText());
    }
}
