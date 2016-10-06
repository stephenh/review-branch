package reviewbranch.apis;

import java.util.Optional;

import reviewbranch.commands.ReviewCommand;

public interface ReviewBoard {

  String createNewRbForCurrentCommit(ReviewCommand args, String currentBranch, Optional<String> dependsOn, Optional<String> bugId);

  void updateRbForCurrentCommit(ReviewCommand args, String rbId, Optional<String> dependsOn);

  void dcommit(String rbId);

}
