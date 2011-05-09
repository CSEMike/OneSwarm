package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.utils.ResizablePanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.utils.ResizablePanelListener;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SerialChatMessage;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.UnknownUserException;

public class ChatDialog extends OneSwarmDialogBox implements Updateable, ResizablePanelListener {

    public static final int DEFAULT_WIDTH = 540;
    public static final int DEFAULT_HEIGHT = 350;

    public static final int MAX_LENGTH = 1024;

    public static final String CSS_CHAT_DIALOG = "os-chat_dialog";
    public static final String CSS_CHAT_OFFLINE = "os-chat_offline";

    String[] keys_by_nick = null;
    Map<Integer, String> listEntryToKey = new HashMap<Integer, String>();
    Map<String, String> keyToNick = new HashMap<String, String>();
    Map<String, FriendInfoLite> keyToFriend = new HashMap<String, FriendInfoLite>();
    private String mSelectedBase64PublicKey;
    private VerticalPanel mChatPanel = new VerticalPanel();
    private final ScrollPanel mChatScroll = new ScrollPanel();
    final private HelpButton mErrorHelpButton = new HelpButton("Some text here");
    final private VerticalPanel mMainRHSVP = new VerticalPanel();
    private HorizontalSplitPanel mainPanel;
    // private Button mSendButton = new Button("Send");
    private final TextBox mTextBox = new TextBox();
    private boolean mSending;
    private FriendInfoLite[] mFriends;

    /**
     * When switching among multiple chats, we should keep the display constant
     * among them
     */
    private final Map<String, VerticalPanel> mKeyToChatPanel = new HashMap<String, VerticalPanel>();
    private final Set<String> mShowingFullHistory = new HashSet<String>();
    private EntireUIRoot mUIRoot;

    private static ChatDialog showing = null;

    public static boolean showing() {
        return showing != null;
    }

    public static boolean tryHide() {
        if (showing != null) {
            if (showing.isWriting()) {

                return false;
            } else {
                showing.hide();
                return true;
            }
        }
        return true;
    }

    public ChatDialog(FriendInfoLite[] friendInfoLites, String inSelectedPublicKey,
            EntireUIRoot root) {

        mSelectedBase64PublicKey = inSelectedPublicKey;
        mFriends = friendInfoLites;
        mUIRoot = root;

        addStyleName(CSS_CHAT_DIALOG);
        setText(msg.swarm_browser_chat());

        GWT.runAsync(new RunAsyncCallback() {
            public void onFailure(Throwable reason) {
                Window.alert("Error loading Chat dialog javascript: " + reason.toString());
            }

            public void onSuccess() {
                // for preloading split code
                if (mFriends == null) {
                    return;
                }

                ChatDialog dlg = onInitialized();
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                if (Cookies.getCookie("os-chat-position") == null) {
                    dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
                } else {
                    try {
                        String[] toks = Cookies.getCookie("os-chat-position").split("_");
                        int left = Integer.parseInt(toks[0]), top = Integer.parseInt(toks[1]), width = Integer
                                .parseInt(toks[2]), height = Integer.parseInt(toks[3]);

                        if (left < 0 || top < 0) {
                            throw new Exception("Bad cookie -- negative left/top: " + left + " / "
                                    + top);
                        }

                        if (width > 25 && height > 25) {
                            dlg.setPopupPosition(left, top);

                            dlg.resized(width, height);

                        } else {
                            throw new Exception("Bad width / height: " + width + " / " + height);
                        }
                    } catch (Exception e) {
                        System.err.println("error parsing chat position cookie: " + e.toString());
                        e.printStackTrace();
                    }
                }
                dlg.setVisible(true);
            }

        });

    }

    protected ChatDialog onInitialized() {

        for (FriendInfoLite f : mFriends) {
            keyToFriend.put(f.getPublicKey(), f);
        }

        mTextBox.getElement().setId("chatTextBox");
        mTextBox.setMaxLength(MAX_LENGTH);

        mainPanel = new HorizontalSplitPanel();

        createLeftWidget();
        mainPanel.setLeftWidget(mUserList);

        createChatPanel(false);
        mChatScroll.setWidget(mChatPanel);
        mChatScroll.setHeight((DEFAULT_HEIGHT - (39 + 16 + 3)) + "px");
        mChatScroll.setAlwaysShowScrollBars(false);

        HorizontalPanel hp = new HorizontalPanel();
        mTextBox.setFocus(true);

        mTextBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == 13) { // enter
                    sendCurrentMessage();
                }
            }
        });

        hp.add(mTextBox);
        hp.setSpacing(3);
        mTextBox.setWidth("96%");
        hp.setWidth("100%");

        mMainRHSVP.add(mChatScroll);
        HorizontalPanel linkAndHelp = new HorizontalPanel();
        linkAndHelp.add(mErrorHelpButton);
        mErrorHelpButton.setVisible(false);
        final Hyperlink linkSwarms = new Hyperlink(msg.chat_link(), "#");
        linkAndHelp.add(linkSwarms);
        linkAndHelp.setSpacing(3);
        linkSwarms.addClickListener(new ClickListener() {

            public void onClick(Widget sender) {
                TorrentInfo[] selected = mUIRoot.getSelectedSwarms();
                if (selected == null) {
                    return;
                }

                if (mTextBox.isEnabled() == false) {
                    return;
                }

                boolean limited_friend = false, f2f_only = false;
                if (keyToFriend.get(mSelectedBase64PublicKey) != null) {
                    if (keyToFriend.get(mSelectedBase64PublicKey).isCanSeeFileList() == false) {
                        limited_friend = true;
                    }
                }

                /**
                 * To compensate for the myriad of representations we have of a
                 * hash. We need to do this on the backend. This is extremely
                 * frustrating to me.
                 */
                String[] tids = new String[selected.length];
                for (int i = 0; i < tids.length; i++) {
                    tids[i] = selected[i].getTorrentID();
                }
                OneSwarmRPCClient.getService().getBase64HashesForOneSwarmHashes(
                        OneSwarmRPCClient.getSessionID(), tids, new AsyncCallback<String[]>() {
                            public void onFailure(Throwable caught) {
                            }

                            public void onSuccess(String[] result) {
                                if (result == null) {
                                    System.err.println("null result on base64 conversion");
                                    return;
                                }

                                for (int i = 0; i < result.length; i++) {
                                    String id = "id:" + result[i];
                                    mTextBox.setText(mTextBox.getText() + id
                                            + (i < result.length - 1 ? " " : ""));
                                    if (mTextBox.getText().length() + id.length() > MAX_LENGTH) {
                                        break;
                                    }
                                }
                            }
                        });

                for (TorrentInfo t : selected) {
                    if (t.isF2FOnly()) {
                        f2f_only = true;
                    }
                }

                if (limited_friend || f2f_only) {
                    StringBuilder errorString = new StringBuilder();
                    if (limited_friend) {
                        errorString.append(msg.chat_no_access_yours());
                    }
                    if (f2f_only) {
                        errorString.append(msg.chat_no_access_friends());
                    }
                    mErrorHelpButton.setText(errorString.toString());
                    mErrorHelpButton.setVisible(true);
                } else {
                    mErrorHelpButton.setVisible(false);
                }
            }
        });
        mMainRHSVP.add(linkAndHelp);
        mMainRHSVP.setCellHorizontalAlignment(linkAndHelp, HorizontalPanel.ALIGN_RIGHT);
        mMainRHSVP.add(hp);
        mMainRHSVP.setCellWidth(hp, "100%");
        mMainRHSVP.setWidth("100%");
        mainPanel.setRightWidget(mMainRHSVP);
        mainPanel.setSplitPosition("25%");

        mainPanel.setWidth(DEFAULT_WIDTH + "px");
        mainPanel.setHeight(DEFAULT_HEIGHT + "px");

        ResizablePanel rp = new ResizablePanel();
        rp.add(mainPanel);
        rp.addResizeListener(this);

        this.setWidget(rp);

        OneSwarmGWT.addToUpdateTask(this);

        return this;
    }

    public void resized(Integer width, Integer height) {

        mainPanel.setWidth(width + "px");
        mainPanel.setHeight(height + "px");

        mUserList.setHeight(height + "px");
        mChatScroll.setHeight((height - (39 + 16 + 3)) + "px");

        setWidth(width + "px");
        setHeight(height + "px");

    }

    public boolean isWriting() {
        return mTextBox.getText().length() > 0;
    }

    @Override
    public void onDetach() {

        Cookies.setCookie("os-chat-position", this.getAbsoluteLeft() + "_" + this.getAbsoluteTop()
                + "_" + this.getOffsetWidth() + "_" + this.getOffsetHeight(),
                OneSwarmConstants.TEN_YEARS_FROM_NOW);

        super.onDetach();
        OneSwarmGWT.removeFromUpdateTask(this);
        showing = null;
    }

    @Override
    public void onAttach() {
        super.onAttach();
        showing = this;
    }

    private void sendCurrentMessage() {
        if (mSending) {
            return;
        }

        mSending = true;
        // mSendButton.setEnabled(false);
        String messageText = mTextBox.getText().trim();

        if (messageText.length() == 0) {
            mTextBox.setText("");
            return;
        }

        SerialChatMessage serialChatMessage = new SerialChatMessage();
        serialChatMessage.setMessage(messageText);

        OneSwarmRPCClient.getService().sendChatMessage(OneSwarmRPCClient.getSessionID(),
                mSelectedBase64PublicKey, serialChatMessage, new AsyncCallback<Boolean>() {
                    public void onSuccess(Boolean result) {
                        mSending = false;
                        nextRPC = 0; // induce immediate refresh

                        if (result == false) {
                            updateFriendStatus(msg.chat_user_offline());
                        }
                    }

                    public void onFailure(Throwable caught) {
                        try {
                            throw caught;
                        } catch (UnknownUserException e) {
                            Window.alert(msg.chat_unknown());
                        } catch (Throwable e) {
                            new ReportableErrorDialogBox(caught.getMessage(), false);
                        }
                        mTextBox.setEnabled(false);
                    }
                });

        mTextBox.setText("");
    }

    private void updateFriendStatus(String msg) {

        if (msg == null) {
            mChatPanel.getWidget(0).setVisible(false);
        } else {
            mChatPanel.getWidget(0).setVisible(true);
            ((Label) mChatPanel.getWidget(0)).setText(msg);
        }
    }

    private void createChatPanel(final boolean include_read) {

        mLastAdded = WhichAdding.First;
        mChatScroll.remove(mChatPanel);

        if (mSelectedBase64PublicKey == null) {
            return;
        }

        boolean force = false;
        if (include_read && !mShowingFullHistory.contains(mSelectedBase64PublicKey)) {
            force = true;
        }

        if (mKeyToChatPanel.containsKey(mSelectedBase64PublicKey) && !force) {
            mChatPanel = mKeyToChatPanel.get(mSelectedBase64PublicKey);
            mChatScroll.setWidget(mChatPanel);
            mChatScroll.scrollToBottom();
            return;
        } else {
            mChatPanel = new VerticalPanel();
            mChatPanel.add(new Label());
            mChatPanel.getWidget(0).setVisible(false);
            mChatPanel.getWidget(0).addStyleName(CSS_CHAT_OFFLINE);

            mChatPanel.setWidth("100%");
            mKeyToChatPanel.put(mSelectedBase64PublicKey, mChatPanel);
        }

        FriendInfoLite f = keyToFriend.get(mSelectedBase64PublicKey);
        if (f != null) {
            if (f.isConnected() == false) {
                updateFriendStatus(msg.chat_user_offline());
            } else {
                updateFriendStatus(null);
            }
        } else {
            // the friend list only contains online friends.
            updateFriendStatus(msg.chat_user_offline());
        }

        if (include_read) {
            mShowingFullHistory.add(mSelectedBase64PublicKey);
        }

        mTextBox.setEnabled(true);

        mChatScroll.setWidget(mChatPanel);

        mChatPanel.add(new Label(msg.loading()));

        OneSwarmRPCClient.getService().getMessagesForUser(OneSwarmRPCClient.getSessionID(),
                mSelectedBase64PublicKey, include_read, 0,
                new AsyncCallback<SerialChatMessage[]>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(final SerialChatMessage[] initialResult) {
                        for (int i = 1; i < mChatPanel.getWidgetCount(); i++) {
                            mChatPanel.remove(i);
                        }

                        Hyperlink show = null;
                        if (!include_read) {
                            show = new Hyperlink(msg.chat_show_previous(), "");
                            show.addClickListener(new ClickListener() {
                                public void onClick(Widget sender) {
                                    createChatPanel(true);
                                    mChatScroll.setWidget(mChatPanel);
                                }
                            });
                        } else {
                            show = new Hyperlink(msg.chat_clear(), "");
                            show.addClickListener(new ClickListener() {
                                public void onClick(Widget sender) {
                                    final String key_shadow = mSelectedBase64PublicKey;
                                    System.out.println("clearing for: " + mSelectedBase64PublicKey
                                            + " / " + keyToNick.get(mSelectedBase64PublicKey));
                                    OneSwarmRPCClient.getService().clearChatLog(
                                            OneSwarmRPCClient.getSessionID(), key_shadow,
                                            new AsyncCallback<Integer>() {
                                                public void onFailure(Throwable caught) {
                                                    caught.printStackTrace();
                                                }

                                                public void onSuccess(Integer result) {
                                                    System.out.println("cleared " + result);
                                                    /**
                                                     * Or else we'll just put up
                                                     * this panel again
                                                     */
                                                    mKeyToChatPanel.remove(key_shadow);
                                                    createChatPanel(true);
                                                }
                                            });
                                }
                            });
                        }

                        mChatPanel.add(show);
                        mChatPanel.setCellWidth(show, "100%");
                        mChatPanel.setCellHorizontalAlignment(show, HorizontalPanel.ALIGN_CENTER);

                        if (initialResult.length > 4) {
                            addMessagesToChatPanel(initialResult);
                        } else if (include_read == false) {
                            // add a little history.
                            OneSwarmRPCClient.getService().getMessagesForUser(
                                    OneSwarmRPCClient.getSessionID(), mSelectedBase64PublicKey,
                                    true, 4, new AsyncCallback<SerialChatMessage[]>() {
                                        public void onFailure(Throwable caught) {
                                            caught.printStackTrace();
                                        }

                                        public void onSuccess(SerialChatMessage[] result) {
                                            /**
                                             * If we've re-requested some
                                             * messages which were unread
                                             * before, mark them again as such
                                             * now.
                                             */
                                            for (SerialChatMessage neu : result) {
                                                for (SerialChatMessage old : initialResult) {
                                                    if (old.getUid() == neu.getUid()) {
                                                        neu.setUnread(old.isUnread());
                                                    }
                                                }
                                            }

                                            addMessagesToChatPanel(result, true);
                                        }
                                    });
                        }
                    }
                });
    }

    protected void addMessagesToChatPanel(SerialChatMessage[] result) {
        addMessagesToChatPanel(result, false);
    }

    private enum WhichAdding {
        First, Incoming, Outgoing
    };

    WhichAdding mLastAdded = WhichAdding.First;

    protected void addMessagesToChatPanel(SerialChatMessage[] result, boolean old) {
        for (SerialChatMessage chat : result) {
            HorizontalPanel hp = new HorizontalPanel();

            Label chatLabel = new Label(chat.getMessage());
            Label timestamp = new Label();

            if (chat.isSent() == false) {
                chatLabel.setText(chatLabel.getText() + " (" + msg.chat_pending() + ")");
            }

            chatLabel.setWordWrap(true);

            transmorphLinks(chatLabel, "http", "<a href=\"#\" onclick=\"window.open('",
                    "', '_blank', '')\">", "</a>");
            transmorphLinks(chatLabel, "id:", "<a href=\"#" + EntireUIRoot.SEARCH_HISTORY_TOKEN,
                    "\">", "</a>");
            transmorphLinks(chatLabel, "oneswarm:", "<a href=\"", "\">", "</a>");

            Date now = new Date();
            Date then = new Date(chat.getTimestamp());

            String stampStr;
            if ((now.getTime() - chat.getTimestamp()) < 86400000 && now.getDate() == then.getDate()) { // 1
                // day
                stampStr = then.getHours() + ":"
                        + (then.getMinutes() < 10 ? "0" + then.getMinutes() : then.getMinutes());
            } else {
                stampStr = (then.getMonth() + 1) + "/" + then.getDate();
            }
            timestamp.setText(stampStr);

            if (old && !(chat.isUnread()) && chat.isSent()) {
                DOM.setStyleAttribute(chatLabel.getElement(), "color", "grey");
                System.out.println(chat.getMessage() + " unread: " + chat.isUnread());
            }

            DOM.setStyleAttribute(timestamp.getElement(), "color", "grey");
            DOM.setStyleAttribute(timestamp.getElement(), "fontSize", "80%");

            if (chat.isOutgoing()) {
                // transition, print our name
                if (mLastAdded.equals(WhichAdding.Incoming) || mLastAdded.equals(WhichAdding.First)) {
                    HTML html = new HTML("<b>" + chat.getNickname() + "</b>");
                    mChatPanel.add(html);
                    // if( old && chat.isUnread() == false ) {
                    DOM.setStyleAttribute(html.getElement(), "color", "grey");
                    // }
                    mChatPanel.setCellHorizontalAlignment(html, HorizontalPanel.ALIGN_RIGHT);
                }

                mLastAdded = WhichAdding.Outgoing;
                hp.add(chatLabel);
                hp.add(timestamp);
                hp.setCellHorizontalAlignment(timestamp, HorizontalPanel.ALIGN_RIGHT);
                mChatPanel.add(hp);
                mChatPanel.setCellHorizontalAlignment(hp, HorizontalPanel.ALIGN_RIGHT);
            } else {
                // transition, print their name
                if (mLastAdded.equals(WhichAdding.Outgoing) || mLastAdded.equals(WhichAdding.First)) {
                    HTML html = new HTML("<b>" + chat.getNickname() + "</b>");
                    mChatPanel.add(html);
                    // if( old && chat.isUnread() == false ) {
                    DOM.setStyleAttribute(html.getElement(), "color", "grey");
                    // }
                    mChatPanel.setCellHorizontalAlignment(html, HorizontalPanel.ALIGN_LEFT);
                }

                mLastAdded = WhichAdding.Incoming;
                hp.add(timestamp);
                hp.setCellHorizontalAlignment(timestamp, HorizontalPanel.ALIGN_LEFT);
                hp.add(chatLabel);
                mChatPanel.add(hp);
                mChatPanel.setCellHorizontalAlignment(hp, HorizontalPanel.ALIGN_LEFT);
            }
            hp.setCellVerticalAlignment(timestamp, VerticalPanel.ALIGN_BOTTOM);
            hp.setCellVerticalAlignment(hp, VerticalPanel.ALIGN_BOTTOM);
            hp.setSpacing(2);
            hp.setCellWidth(timestamp, "35px");
        }

        mChatScroll.scrollToBottom();
    }

    private void transmorphLinks(Label chatLabel, String tag, String prefix, String suffix,
            String close) {
        try {
            String innerHTML = chatLabel.getElement().getInnerHTML();
            System.out.println("inner html is: " + innerHTML);
            StringBuilder out = new StringBuilder();
            int curr = 0;
            boolean done = false;
            while (!done) {
                curr = innerHTML.indexOf(tag, curr);
                if (curr == -1) {
                    done = true;
                    break;
                }

                out.append(innerHTML.substring(0, curr) + prefix);
                int end = innerHTML.indexOf(' ', curr);
                if (end == -1) {
                    end = innerHTML.length();
                }
                out.append(innerHTML.subSequence(curr, end) + suffix
                        + innerHTML.subSequence(curr, end) + close);
                curr = out.length();
                out.append(innerHTML.subSequence(end, innerHTML.length()));
                innerHTML = out.toString();
            }
            // System.out.println("transmorphed: " + innerHTML);
            chatLabel.getElement().setInnerHTML(innerHTML);
        } catch (Exception e) {
            // eat exception, do nothing to chat label itself.
            e.printStackTrace();
        }
    }

    final ListBox mUserList = new ListBox();
    ChangeListener userChangeListener = null;

    private void createLeftWidget() {

        mUserList.setVisibleItemCount(10);
        mUserList.setHeight(DEFAULT_HEIGHT + "px");
        mUserList.setWidth("100%");

        userChangeListener = new ChangeListener() {
            public void onChange(Widget sender) {
                System.out.println("change listener " + mUserList.getSelectedIndex() + " / ");

                if (mUserList.getSelectedIndex() == -1) {
                    mSelectedBase64PublicKey = null;
                    createChatPanel(false);
                    ChatDialog.this.setText(msg.swarm_browser_chat());
                    return;
                }

                mSelectedBase64PublicKey = keys_by_nick[mUserList.getSelectedIndex()];
                ChatDialog.this.setText(msg.swarm_browser_chat() + ": "
                        + keyToNick.get(mSelectedBase64PublicKey));
                mUserList.setItemText(mUserList.getSelectedIndex(),
                        keyToNick.get(mSelectedBase64PublicKey));

                System.out.println(mSelectedBase64PublicKey);

                createChatPanel(false);
            }
        };

        mUserList.addChangeListener(userChangeListener);

        mUserList.addItem("Loading...");
        OneSwarmRPCClient.getService().getUsersWithMessages(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<HashMap<String, String[]>>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(final HashMap<String, String[]> result) {
                        mUserList.clear();

                        /**
                         * Result is base64Key -> String[]{name, unread
                         * messages}
                         */

                        /**
                         * Build up the list of users with which we 1) have
                         * chatted (i.e., with history) or 2) could chat (those
                         * that have chat capability). We get 1) from the friend
                         * list passed to the constructor and 2) from an RPC to
                         * the backend DB.
                         */
                        java.util.Set<String> userSet = new HashSet<String>();
                        userSet.addAll(result.keySet());
                        for (FriendInfoLite f : mFriends) {
                            if (f.isSupportsChat() && f.isAllowChat()) {
                                userSet.add(f.getPublicKey());
                            }
                        }

                        for (String key : userSet) {
                            if (result.containsKey(key)) {
                                keyToNick.put(key, result.get(key)[0]);
                            } else {
                                for (FriendInfoLite f : mFriends) {
                                    if (f.getPublicKey().equals(key)) {
                                        keyToNick.put(key, f.getName());
                                    }
                                }
                            }
                        }

                        keys_by_nick = keyToNick.keySet().toArray(new String[0]);
                        Arrays.sort(keys_by_nick, new Comparator<String>() {
                            public int compare(String o1, String o2) {
                                return keyToNick.get(o1).compareTo(keyToNick.get(o2));
                            }
                        });

                        mUserList.setVisibleItemCount(Math.max(keys_by_nick.length, 10));

                        boolean picked = false;
                        for (String key : keys_by_nick) {
                            String unreadStr = "";
                            if (result.containsKey(key)) {
                                if (result.get(key)[1].equals("0") == false) {
                                    unreadStr = " - " + result.get(key)[1];
                                }
                            }
                            mUserList.addItem(keyToNick.get(key) + unreadStr);

                            if (mSelectedBase64PublicKey != null) {
                                if (mSelectedBase64PublicKey.equals(key)) {
                                    mUserList.setSelectedIndex(mUserList.getItemCount() - 1);
                                    ChatDialog.this.setText(msg.swarm_browser_chat() + ": "
                                            + keyToNick.get(mSelectedBase64PublicKey));
                                    mUserList.setItemText(mUserList.getSelectedIndex(),
                                            keyToNick.get(mSelectedBase64PublicKey));
                                }
                            } else if (unreadStr.equals("") == false && !picked) {
                                /**
                                 * If nothing selected, at least prefer
                                 * something that has unread messages
                                 */
                                picked = true;
                                mUserList.setSelectedIndex(mUserList.getItemCount() - 1);
                                userChangeListener.onChange(mUserList);
                            }
                        }
                    }

                }); // getUsersWithMessages RPC

    }

    long nextRPC = 0;

    public void update(int count) {
        if (nextRPC < System.currentTimeMillis()) {
            nextRPC = System.currentTimeMillis() + 5 * 1000;
            OneSwarmRPCClient.getService().getMessagesForUser(OneSwarmRPCClient.getSessionID(),
                    mSelectedBase64PublicKey, false, 0, new AsyncCallback<SerialChatMessage[]>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(SerialChatMessage[] result) {
                            if (result.length > 0) {
                                addMessagesToChatPanel(result);
                            }
                            nextRPC = System.currentTimeMillis() + 1000;
                        }
                    });
        }
    }
}
