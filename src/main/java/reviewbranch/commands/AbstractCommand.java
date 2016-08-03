package reviewbranch.commands;

import java.util.List;

import reviewbranch.ReviewBranch;
import reviewbranch.apis.Git;
import reviewbranch.apis.ReviewBoard;

/**
 * An interface for {@link ReviewBranch} commands.
 */
public abstract class AbstractCommand {

  public abstract void run(Git git, ReviewBoard rb);

  public void ensureGitNotesConfigured(Git git) {
    addConfigIfNeeded(git, "notes.displayRef", "refs/notes/reviewid");
    addConfigIfNeeded(git, "notes.displayRef", "refs/notes/reviewlasthash");
    addConfigIfNeeded(git, "notes.rewriteRef", "refs/notes/reviewid");
    addConfigIfNeeded(git, "notes.rewriteRef", "refs/notes/reviewlasthash");
  }

  private static void addConfigIfNeeded(Git git, String key, String value) {
    List<String> current = git.getMultipleValueConfig(key);
    if (!current.contains(value)) {
      git.addMultipleValueConfig(key, value);
    }
  }

}
