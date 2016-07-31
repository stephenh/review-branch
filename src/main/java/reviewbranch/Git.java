package reviewbranch;

import java.util.List;

public interface Git {

  String getCurrentBranch();

  List<String> getRevisionsFromOriginMaster();

  void checkout(String revision);

  String getCurrentCommitMessage();

  void amendCurrentCommitMessage(String newMessage);

}
