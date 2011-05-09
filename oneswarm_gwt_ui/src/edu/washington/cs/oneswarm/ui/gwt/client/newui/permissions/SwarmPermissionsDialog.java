package edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions;

import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.TorrentDownloaderDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.groups.GroupsManagementTab;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.swarms.SwarmPermissionsTab;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class SwarmPermissionsDialog extends OneSwarmDialogBox {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 550;
    private TorrentInfo[] mTorrents;

    HTML selectLabel = new HTML("");

    public SwarmPermissionsDialog(TorrentInfo[] torrents, TorrentInfo inSelected) {
        super();

        if (torrents.length == 0) {
            System.err.println("tried to manage permissions with no torrents!");
            (new Exception()).printStackTrace();
            return;
        }

        int selectedIndex = 0;

        if (inSelected != null) {
            for (int i = 0; i < torrents.length; i++) {
                if (torrents[i].equals(inSelected)) {
                    selectedIndex = i;
                }
            }
        }

        this.setText(msg.visibility_header());

        DecoratedTabPanel tabs = new DecoratedTabPanel();
        tabs.addStyleName(TorrentDownloaderDialog.CSS_F2F_TABS);

        mTorrents = torrents;

        VerticalPanel mainPanel = new VerticalPanel();

        selectLabel.addStyleName(CSS_DIALOG_HEADER);
        selectLabel.setWidth(WIDTH + "px");
        mainPanel.setHeight(HEIGHT + "px");
        mainPanel.add(selectLabel);

        final SimplePanel swarms_wrapper = new SimplePanel();
        final SimplePanel groups_wrapper = new SimplePanel();

        final int index_shadow = selectedIndex;
        tabs.addTabListener(new TabListener() {
            public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
                return true;
            }

            public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
                /**
                 * We recreate these panels each time to keep the list of
                 * swarms/friends/groups in sync
                 */
                switch (tabIndex) {
                case 0:
                    selectLabel.setHTML(msg.visibility_specify_HTML());
                    swarms_wrapper.setWidget(new SwarmPermissionsTab(mTorrents, index_shadow));
                    break;

                case 1:
                    selectLabel.setText(msg.visibility_groups_msg());
                    groups_wrapper.setWidget(new GroupsManagementTab());
                    break;
                }
            }
        });

        tabs.setWidth("100%");

        // tabs.add(new SwarmPermissionsTab(mTorrents, selectedIndex),
        // "Swarms");
        // tabs.add(new GroupsManagementTab(), "Groups");

        tabs.add(swarms_wrapper, msg.visibility_swarms_tab());
        tabs.add(groups_wrapper, msg.visibility_groups_tab());

        // mainPanel.add(new MembershipList(true, Arrays.asList(new
        // String[]{"this", "is", "a", "test"}), (new ArrayList())));

        tabs.selectTab(0);

        mainPanel.add(tabs);
        mainPanel.setCellVerticalAlignment(tabs, VerticalPanel.ALIGN_TOP);

        this.setWidget(mainPanel);
    }

}
