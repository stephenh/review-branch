package reviewbranch;

import java.util.List;

public interface Git {

  String getCurrentBranch();

  List<String> getRevisionsFromOriginMaster();

  void checkout(String revision);

  void resetHard(String revision);

  void cherryPick(String revision);

  String getCurrentCommitMessage();

  void amendCurrentCommitMessage(String newMessage);

}
