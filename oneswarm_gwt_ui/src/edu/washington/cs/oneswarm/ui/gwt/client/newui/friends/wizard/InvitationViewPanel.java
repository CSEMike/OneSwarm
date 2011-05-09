package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.datepicker.client.DatePicker;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInvitationLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class InvitationViewPanel extends FriendsImportWithBack {
    private final InvitationViewStep firstStep;

    public InvitationViewPanel(FriendsImportCallback fwCallback) {
        super();
        this.fwCallback = fwCallback;
        firstStep = new InvitationViewStep();
        setFirstStep(firstStep);
    }

    public void refresh() {
        firstStep.refresh();
    }

    private class InvitationViewStep extends FriendsImportWithBackStep {

        private final VerticalPanel sentTablePanel = new VerticalPanel();
        private final VerticalPanel redeemedTablePanel = new VerticalPanel();

        public InvitationViewStep() {

            /**
             * The table for sent invitations
             */

            Label sentLabel = new HTML(msg.add_friends_invite_view_sent_HTML());
            sentLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
            sentLabel.setWidth(WIDTH + "px");
            this.add(sentLabel);
            sentTablePanel.setWidth(WIDTH + "px");
            this.add(sentTablePanel);
            /**
             * The table for redeemed invitations
             */
            Label redeemedLabel = new HTML(msg.add_friends_invite_view_redeemed_HTML());
            redeemedLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
            redeemedLabel.setWidth(WIDTH + "px");
            this.add(redeemedLabel);
            redeemedTablePanel.setWidth(WIDTH + "px");
            this.add(redeemedTablePanel);

        }

        public void refresh() {

            sentTablePanel.clear();
            OneSwarmRPCClient.getService().getSentFriendInvitations(
                    OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<ArrayList<FriendInvitationLite>>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(ArrayList<FriendInvitationLite> result) {
                            FriendInvitationLite[] invitations = result
                                    .toArray(new FriendInvitationLite[result.size()]);
                            if (invitations.length > 0) {
                                sentTablePanel.add(new InvitationTable(invitations, true) {
                                    public void refresh() {
                                        InvitationViewStep.this.refresh();
                                    }
                                });
                            } else {
                                Label l = new Label(msg.add_friends_invite_view_no_sent());
                                sentTablePanel.add(l);
                                sentTablePanel.setCellHorizontalAlignment(l,
                                        VerticalPanel.ALIGN_CENTER);
                            }
                        }
                    });

            redeemedTablePanel.clear();
            OneSwarmRPCClient.getService().getRedeemedFriendInvitations(
                    OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<ArrayList<FriendInvitationLite>>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(ArrayList<FriendInvitationLite> result) {

                            FriendInvitationLite[] invitations = result
                                    .toArray(new FriendInvitationLite[result.size()]);
                            if (invitations.length > 0) {
                                redeemedTablePanel.add(new InvitationTable(invitations, false) {
                                    public void refresh() {
                                        InvitationViewStep.this.refresh();
                                    }
                                });
                            } else {
                                Label l = new Label(msg.add_friends_invite_view_no_redeemed());
                                redeemedTablePanel.add(l);
                                redeemedTablePanel.setCellHorizontalAlignment(l,
                                        VerticalPanel.ALIGN_CENTER);
                            }

                        }
                    });

        }

        @Override
        public FriendsImportWithBackStep createNextPanel() {
            return null;
        }

        @Override
        public String getNextButtonText() {
            return msg.button_done();
        }

    }

    private final FriendsImportCallback fwCallback;

    private static class InvitationTable extends ScrollTable {

        class FriendInvitationCheckBox extends CheckBox {
            FriendInvitationLite f;

            public FriendInvitationCheckBox(FriendInvitationLite f) {
                super();
                this.f = f;
            }

            public FriendInvitationLite getFriendInvitationLite() {
                return f;
            }
        }

        enum Column {
            CHECKBOX("", 0, 10), NICKNAME(msg.add_friends_invite_view_table_nickname(), 1, 45), CREATED(
                    msg.add_friends_invite_view_table_created(), 2, 45), STATUS(msg
                    .add_friends_invite_view_table_status(), 3, 45), EXPIRES(msg
                    .add_friends_invite_view_table_expires(), 4, 45);

            Column(String name, int column, int width) {
                this.nickname = name;
                this.width = width;
                this.column = column;
            }

            final int width;
            final int column;
            final String nickname;
        }

        private ArrayList<FriendInvitationLite> getSelectedInvitations() {
            ArrayList<FriendInvitationLite> out = new ArrayList<FriendInvitationLite>();

            for (int i = 0; i < mData.getRowCount(); i++) {
                FriendInvitationCheckBox cb = (FriendInvitationCheckBox) mData.getWidget(i, 0);
                if (cb.getValue()) {
                    out.add(cb.f);
                }
            }

            // for( int i=0; i<friends.length; i++ ) {
            // if( selectors.get(i).getValue() ) {
            // out.add(friends[i]);
            // }
            // }
            return out;
        }

        public void removeClicked() {

            ArrayList<FriendInvitationLite> selectedInvitations = getSelectedInvitations();

            if (Window.confirm(msg.add_friends_invite_view_confirm_delete(selectedInvitations
                    .size()))) {

                OneSwarmRPCClient.getService().deleteFriendInvitations(
                        OneSwarmRPCClient.getSessionID(), selectedInvitations,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                OneSwarmGWT.log("delete friend: got error");
                            }

                            public void onSuccess(Void result) {
                                OneSwarmGWT.log("friend deleted");

                            }
                        });
            }
        }

        private final FixedWidthGrid mData;
        private final FixedWidthFlexTable mHeader;

        public InvitationTable(FriendInvitationLite[] invitations, boolean createdLocally) {
            super(new FixedWidthGrid(0, createdLocally ? Column.values().length
                    : Column.values().length - 1), new FixedWidthFlexTable());
            mData = getDataTable();
            mHeader = getHeaderTable();
            mData.setWidth("100%");
            mHeader.setWidth("100%");

            setScrollPolicy(ScrollPolicy.DISABLED);
            mData.setSelectionPolicy(SelectionPolicy.MULTI_ROW);
            setResizePolicy(ResizePolicy.FILL_WIDTH);
            setupHeader(createdLocally);

            TableListener listener = new TableListener() {
                public void onCellClicked(SourcesTableEvents check, int row, int column) {
                    if (column >= 1) {

                        FriendInvitationLite invitation = ((FriendInvitationCheckBox) mData
                                .getWidget(row, 0)).f;
                        int width = InvitationTable.this.getOffsetWidth();
                        int height = InvitationTable.this.getOffsetHeight();
                        OneSwarmDialogBox dlg = new InvitationDialog(invitation, width, height) {
                            public void hide() {
                                refresh();
                                super.hide();
                            }
                        };
                        dlg.show();
                        dlg.setVisible(false);
                        dlg.center();
                        dlg.setPopupPosition(dlg.getAbsoluteLeft() + 40,
                                InvitationTable.this.getAbsoluteTop());
                        dlg.setVisible(true);

                    }
                }
            };
            mData.addTableListener(listener);

            for (int rowoffset = 0; rowoffset < invitations.length; rowoffset++) {
                final FriendInvitationLite invitation = invitations[rowoffset];
                FriendInvitationCheckBox selector = new FriendInvitationCheckBox(invitation);
                mData.insertRow(rowoffset);
                mData.setWidget(rowoffset, 0, selector);

                mData.setText(rowoffset, Column.NICKNAME.column, invitation.getName());
                mData.setText(rowoffset, Column.CREATED.column, StringTools.formatDateAppleLike(
                        new Date(invitation.getCreatedDate()), true));

                String statusText = invitation.getStatusText();
                mData.setText(rowoffset, Column.STATUS.column, statusText);

                if (createdLocally) {
                    String expires;
                    if (invitation.getMaxAge() > 0) {

                        expires = StringTools.formatDateAppleLike(
                                new Date(invitation.getCreatedDate() + invitation.getMaxAge()),
                                true);
                    } else {
                        expires = "never";
                    }
                    mData.setText(rowoffset, 4, expires);
                }
            }
        }

        private void setupHeader(boolean useExpires) {
            int numColumns = useExpires ? Column.values().length : Column.values().length - 1;

            for (int i = 0; i < numColumns; i++) {
                Column column = Column.values()[i];
                mHeader.setText(0, i, column.nickname);
                mHeader.setColumnWidth(i, column.width);
                mData.setColumnWidth(i, column.width);
            }
        }

        protected void refresh() {

        }

    }

    @Override
    protected void onLastBack() {
        fwCallback.back();
    }

    @Override
    protected void onLastNext() {
        fwCallback.cancel();
    }

    static class InvitationDialog extends OneSwarmDialogBox {
        private final Button cancelButton = new Button(msg.button_cancel());
        private final Button saveButton = new Button(msg.button_save());
        private final Button deleteButton = new Button(msg.add_friends_invite_view_delete());
        private final InvitationDetailsPanel invPanel;
        private final FriendInvitationLite invitation;

        public InvitationDialog(FriendInvitationLite invitation, int width, int height) {
            super(false, true, true);
            this.invitation = invitation;
            setText(msg.add_friends_invite_view_edit(invitation.getName()));
            VerticalPanel p = new VerticalPanel();
            invPanel = new InvitationDetailsPanel(invitation);
            p.add(invPanel);

            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setWidth("100%");

            deleteButton.addClickHandler(this);
            buttonPanel.add(deleteButton);
            buttonPanel.setCellHorizontalAlignment(deleteButton, HorizontalPanel.ALIGN_LEFT);

            HorizontalPanel okCancelPanel = new HorizontalPanel();
            okCancelPanel.setSpacing(3);

            cancelButton.addClickHandler(this);
            okCancelPanel.add(cancelButton);

            saveButton.addClickHandler(this);
            okCancelPanel.add(saveButton);

            buttonPanel.add(okCancelPanel);
            buttonPanel.setCellHorizontalAlignment(okCancelPanel, HorizontalPanel.ALIGN_RIGHT);

            p.add(buttonPanel);
            super.setWidget(p);
            super.setWidth(width + "px");
            super.setHeight(height + "px");
        }

        public void onClick(ClickEvent event) {
            if (event.getSource().equals(cancelButton)) {
                hide();
            } else if (event.getSource().equals(saveButton)) {
                invPanel.save();

            } else if (event.getSource().equals(deleteButton)) {
                if (Window.confirm(msg.add_friends_invite_view_confirm_delete_single())) {
                    ArrayList<FriendInvitationLite> l = new ArrayList<FriendInvitationLite>();
                    l.add(invitation);
                    OneSwarmRPCClient.getService().deleteFriendInvitations(
                            OneSwarmRPCClient.getSessionID(), l, new AsyncCallback<Void>() {
                                public void onFailure(Throwable caught) {
                                    OneSwarmGWT.log("delete invite: got error");
                                }

                                public void onSuccess(Void result) {
                                    OneSwarmGWT.log("invite deleted");
                                    hide();
                                }
                            });
                }
            } else {
                super.onClick(event);
            }
        }

        class InvitationDetailsPanel extends VerticalPanel {
            final TextBox nickBox = new TextBox();
            final DatePickerTextBox expiresPicker;
            private final FriendInvitationLite invitation;

            public InvitationDetailsPanel(FriendInvitationLite invitation) {
                this.invitation = invitation;

                /**
                 * nickname
                 */
                HorizontalPanel nickPanel = new HorizontalPanel();
                nickPanel.setSpacing(3);
                nickPanel.setWidth(WIDTH + "px");

                Label nickLabel = new Label(msg.friend_properties_nickname_label());
                nickLabel.setWidth("100%");
                nickPanel.add(nickLabel);
                nickPanel.setCellVerticalAlignment(nickLabel, VerticalPanel.ALIGN_MIDDLE);
                nickPanel.setCellWidth(nickLabel, "110px");
                nickPanel.add(nickBox);
                nickBox.setWidth("100%");
                nickBox.setText(invitation.getName());

                HelpButton nameHelp = new HelpButton(InvitationCreatePanel.FRIEND_INVITE_NAME_HELP);
                nickPanel.add(nameHelp);
                nickPanel.setCellVerticalAlignment(nameHelp, VerticalPanel.ALIGN_MIDDLE);
                nickPanel.setCellHorizontalAlignment(nameHelp, HorizontalPanel.ALIGN_RIGHT);
                this.add(nickPanel);

                /**
                 * expires
                 */
                expiresPicker = new DatePickerTextBox(new Date(invitation.getCreatedDate()
                        + invitation.getMaxAge()));

                HorizontalPanel expiresPanel = new HorizontalPanel();
                expiresPanel.setSpacing(3);
                expiresPanel.setWidth(WIDTH + "px");

                Label expiresLabel = new Label(msg.add_friends_invite_view_expires());
                expiresLabel.setWidth("100%");
                expiresPanel.add(expiresLabel);
                expiresPanel.setCellVerticalAlignment(expiresLabel, VerticalPanel.ALIGN_MIDDLE);
                expiresPanel.setCellWidth(expiresLabel, "110px");
                expiresPanel.add(expiresPicker);
                expiresPicker.setWidth("100%");

                HelpButton expiresHelp = new HelpButton(msg.add_friends_invite_view_expires_help());
                expiresPanel.add(expiresHelp);
                expiresPanel.setCellVerticalAlignment(expiresHelp, VerticalPanel.ALIGN_MIDDLE);
                expiresPanel.setCellHorizontalAlignment(expiresHelp, HorizontalPanel.ALIGN_RIGHT);
                this.add(expiresPanel);

                /**
                 * invitation key
                 */
                HTML invitationLabel = new HTML(msg.add_friends_invite_view_invitation_key());
                invitationLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
                invitationLabel.setWidth(WIDTH + "px");
                this.add(invitationLabel);

                Label codeLabel = new Label("" + invitation.getKey());
                codeLabel.addStyleName("friend_invitation-code");
                codeLabel.addStyleName("friend_invitation-code-padding");
                codeLabel.setWidth(WIDTH + "px");
                this.add(codeLabel);

            }

            public void save() {
                invitation.setName(nickBox.getText());
                invitation.setMaxAge(Math.max(0,
                        expiresPicker.getValue().getTime() - invitation.getCreatedDate()));
                OneSwarmRPCClient.getService().updateFriendInvitations(
                        OneSwarmRPCClient.getSessionID(), invitation, new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(Void result) {
                                InvitationDialog.this.hide();
                            }
                        });
            }
        }

    }

    static class DatePickerTextBox extends VerticalPanel {
        DatePicker datePicker = new DatePicker();

        public DatePickerTextBox(Date defaultValue) {
            final TextBox expiresText = new TextBox();
            expiresText.setWidth("100%");
            expiresText.addStyleName("os-date-picker-label");
            expiresText.setReadOnly(true);
            final PopupPanel p = new PopupPanel(true);
            p.add(datePicker);
            p.addStyleName("os-date-picker-popup");
            expiresText.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    p.setPopupPosition(expiresText.getAbsoluteLeft(), expiresText.getAbsoluteTop());
                    p.show();
                }
            });

            // Set the value in the text box when the user selects a date
            datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
                public void onValueChange(ValueChangeEvent<Date> event) {
                    Date date = event.getValue();
                    String dateString = StringTools.formatDateAppleLike(date, true);
                    expiresText.setText(dateString);
                    p.hide();
                }
            });
            // Set the default value
            datePicker.setValue(defaultValue, true);

            this.add(expiresText);
        }

        public Date getValue() {
            return datePicker.getValue();
        }
    }
}
