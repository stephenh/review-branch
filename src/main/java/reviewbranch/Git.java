package reviewbranch;

import java.util.List;

public interface Git {

  List<String> getRevisionsFromOriginMaster();

  void checkout(String revision);

  String getCurrentCommitMessage();

  void amendCurrentCommitMessage(String newline);

}
