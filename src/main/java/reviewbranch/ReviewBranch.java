package reviewbranch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.github.rvesse.airline.help.Help;

import reviewbranch.apis.Git;
import reviewbranch.apis.GitImpl;
import reviewbranch.apis.ReviewBoard;
import reviewbranch.apis.ReviewBoardImpl;
import reviewbranch.commands.AbstractCommand;
import reviewbranch.commands.DCommitCommand;
import reviewbranch.commands.MergeApprovedCommand;
import reviewbranch.commands.ReviewCommand;

/**
 * Creates RBs for a branch, one RB per commit.
 */
public class ReviewBranch {

  private static final Logger log = LoggerFactory.getLogger(ReviewBranch.class);

  public static void main(String[] stringArgs) {
    CliBuilder<Object> b = Cli.<Object> builder("review-branch").withDescription("creates lots of RBs");
    b.withCommand(ReviewCommand.class);
    b.withCommand(DCommitCommand.class);
    b.withCommand(MergeApprovedCommand.class);
    b.withDefaultCommand(Help.class);

    Object command = b.build().parse(stringArgs);
    if (command instanceof AbstractCommand) {
      Git git = new GitImpl();
      ReviewBoard rb = new ReviewBoardImpl();
      if (!git.isWorkingCopyClean()) {
        log.error("Your working copy is not clean; ensure all changes are committed or stashed.");
      } else {
        ((AbstractCommand) command).ensureGitNotesConfigured(git);
        ((AbstractCommand) command).run(git, rb);
      }
    } else {
      ((Runnable) command).run();
    }
  }
}
