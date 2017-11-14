package reviewbranch.apis;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReviewId {

  private static final Pattern rbRegex = Pattern.compile("RB=(\\d+)");

  public static Optional<String> getFromNoteOrCommitMessage(Git git, String message) {
    Optional<String> rbId = git.getNote("reviewid");
    // See if this is a rebased commit with an existing reviewid
    if (!rbId.isPresent()) {
      if (message != null) {
        Matcher m = rbRegex.matcher(message);
        if (m.find()) {
          rbId = Optional.of(m.group(1));
        }
      }
    }
    return rbId;
  }

}
