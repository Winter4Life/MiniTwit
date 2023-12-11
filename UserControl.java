import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class UserControl implements TweetObserver,IDVisitor {

    @FXML
    private ListView<String> currentFollow;

    @FXML
    private ListView<String> newsFeed;
    private static int totalMessages = 0;

    @FXML
    private TextArea tweet;

    @FXML
    private TextArea userID;
    private String userName;
    private static final Map<String, UserState> userStates = new HashMap<>();

    public static Map<String, UserState> getUserStates() {
        return userStates;
    }

    public void visit(String visitorID, String formattedMessage) {
        UserState userState = userStates.get(visitorID);
        if (userState != null) {
            userState.getNewsFeed().add(formattedMessage);
        }
    }

    @Override
    public void update(String userName, String tweet) {
        newsFeed.getItems().add(tweet);
    }

    public void initData(Stage stage, String userName, ObservableList<String> existingUserIDs) {
        this.userName = userName;
        stage.setTitle("User Control Panel: " + userName);

        UserState userState = userStates.getOrDefault(userName, new UserState());
        currentFollow.setItems(userState.getCurrentFollow());
        newsFeed.setItems(userState.getNewsFeed());
    }


    @FXML
    void btnFollow(ActionEvent event) {
        String followerID = userID.getText().trim();

        // Check if the entered user ID exists in the list of all users
        if (AdminControl.getAllUsers().contains(followerID)) {
            if (!followerID.equals(userName)) {
                currentFollow.getItems().add(followerID);
                saveUserState();

                // Update last update time when a user follows someone
                AdminControl.getInstance().updateLastUpdateTime(userName, System.currentTimeMillis());
            } else {
                showPopup("Error", "Cannot follow yourself.");
            }
        } else {
            showPopup("Error", "No existing user with ID: " + followerID);
        }
        AdminControl.registerObserver(followerID, this);
        userID.clear();
    }


    // Helper Method to display follow in newsfeed
    private void showPopup(String title, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    @FXML
    void btnPost(ActionEvent event) {
        String message = tweet.getText().trim();
        if (!message.isEmpty()) {
            String formattedMessage = userName + ": " + message;
            newsFeed.getItems().add(formattedMessage);

            totalMessages++;

            informFollowersAboutTweet(userName, formattedMessage);
            tweet.clear();

            AdminControl.notifyObservers(userName, formattedMessage);
            saveUserState();

            AdminControl.getInstance().updateLastUpdateTime(userName, System.currentTimeMillis());
        }
    }


    private void informFollowersAboutTweet(String userName, String formattedMessage) {
        for (Map.Entry<String, UserState> entry : userStates.entrySet()) {
            String follower = entry.getKey();
            UserState followerState = entry.getValue();

            if (followerState.getCurrentFollow().contains(userName)) {
                // Update the newsFeed of followers
                followerState.getNewsFeed().add(formattedMessage);
                userStates.put(follower, followerState);

                followerState.accept(this, formattedMessage);
            }
        }
    }
    public static int getTotalMessages() {
        return totalMessages;
    }  

    // To save data if usercontrol panel is closed
    private void saveUserState() {
        UserState userState = new UserState();
        userState.setCurrentFollow(FXCollections.observableArrayList(currentFollow.getItems()));
        userState.setNewsFeed(FXCollections.observableArrayList(newsFeed.getItems()));
        userStates.put(userName, userState);
    }

    public static class UserState {
        private ObservableList<String> currentFollow = FXCollections.observableArrayList();
        private ObservableList<String> newsFeed = FXCollections.observableArrayList();

        public ObservableList<String> getCurrentFollow() {
            return currentFollow;
        }

        public void setCurrentFollow(ObservableList<String> currentFollow) {
            this.currentFollow = currentFollow;
        }

        public ObservableList<String> getNewsFeed() {
            return newsFeed;
        }

        public void setNewsFeed(ObservableList<String> newsFeed) {
            this.newsFeed = newsFeed;
        }

        public void accept(IDVisitor visitor, String formattedMessage) {
            for (String userID : currentFollow) {
                visitor.visit(userID, formattedMessage);
            }
        }
    }

    public void setTreeItem(TreeItem<String> userItem) {
    }
}