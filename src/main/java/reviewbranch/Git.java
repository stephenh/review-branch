package reviewbranch;

import java.util.List;
import java.util.Optional;

public interface Git {

  String getCurrentBranch();

  String getCurrentTreeHash();

  String getCurrentCommitMessage();

  void amendCurrentCommitMessage(String newMessage);

  List<String> getRevisionsFromOriginMaster();

  void checkout(String revision);

  void resetHard(String revision);

  void cherryPick(String revision);

  List<String> getMultipleValueConfig(String key);

  void addMultipleValueConfig(String key, String value);

  Optional<String> getNote(String ref);

  void setNote(String ref, String value);

}
