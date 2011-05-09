package edu.washington.cs.oneswarm.test.integration;

import java.util.List;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.chat.Chat;
import edu.washington.cs.oneswarm.f2f.chat.ChatDAO;
import edu.washington.cs.oneswarm.test.util.ConditionWaiter;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.test.util.TwoProcessTestBase;

public class ChatTest extends TwoProcessTestBase {

    private static Logger logger = Logger.getLogger(ChatTest.class.getName());

    @Test
    public void testSendReceiveChat() throws Exception {
        /*
         * Test plan: Send a chat message using the Web UI and verify that it is
         * displayed in the web UI of the remote host.
         */

        selenium.openWindow("http://127.0.0.1:4000/", "jvm");
        selenium.openWindow("http://127.0.0.1:3000/", "local");

        selenium.selectWindow("jvm");

        // Wait for the friends list AJAX load to complete
        TestUtils.awaitElement(selenium, "//td[2]/div/div");

        // Double-click the first friend in the list, opens chat.
        selenium.click("//td[2]/div/div");
        selenium.click("//td[2]/div/div");

        // Send chat message
        final String chatMessage = "ChatMessage JVM to Local";
        TestUtils.awaitElement(selenium, "chatTextBox");
        selenium.focus("chatTextBox");
        selenium.typeKeys("chatTextBox", chatMessage);
        selenium.keyPress("chatTextBox", "13");

        // Verify local display of the chat message.
        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                return selenium.isTextPresent(chatMessage);
            }
        }, 5000).await();

        // Switch to the other instance
        selenium.selectWindow("local");

        // Verify notification presence -- this could take up to 10 seconds
        // since
        // we have a 10 seconds poll (See {@code FriendListPanel.java}).
        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                return selenium.isElementPresent("link=1 unread message");
            }
        }, 15000).await();

        // Click to bring up chat box
        selenium.click("link=1 unread message");

        // Verify message in chat box
        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                return selenium.isTextPresent("ChatMessage JVM to Local");
            }
        }, 5000).await();

        // Finally, verify that this message was stored in our local database
        List<Chat> storedMessage = ChatDAO.get().getMessagesForUser(localOneSwarm.getPublicKey(),
                true, 1);
        Assert.assertEquals(storedMessage.get(0).getMessage(), chatMessage);

        selenium.close();
    }

    /** Closes the web UI */
    @After
    public void tearDownTest() throws Exception {
        selenium.close();
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(ChatTest.class);
    }
}
