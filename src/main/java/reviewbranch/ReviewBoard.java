package reviewbranch;

import java.util.Optional;

import reviewbranch.ReviewBranch.ReviewArgs;

public interface ReviewBoard {

  String createNewRbForCurrentCommit(ReviewArgs args, String currentBranch, Optional<String> dependsOn);

  void updateRbForCurrentCommit(ReviewArgs args, String rbId, Optional<String> dependsOn);

  void dcommit(String rbId);

}
