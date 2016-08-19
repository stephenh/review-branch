package reviewbranch.commands;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rvesse.airline.annotations.Command;

import reviewbranch.apis.Git;
import reviewbranch.apis.ReviewBoard;

@Command(name = "merge-approved", description = "Merges commits whose RBs been approved to master")
public class MergeApprovedCommand extends AbstractCommand {

  private static final Logger log = LoggerFactory.getLogger(MergeApprovedCommand.class);
  private static final Pattern approvalPattern = Pattern.compile("\\nA=[^\\n]+");
  
  @Override
  public void run(Git git, ReviewBoard rb) {
    List<String> revs = git.getRevisionsFromOriginMaster();
    log.info("Found revs {}", revs);

    Optional<String> maxApprovedRev = Optional.empty();

    for (String rev : revs) {
      log.info("Resetting to {}", rev);
      git.resetHard(rev);
      String commitMessage = git.getCurrentCommitMessage();
      if (approvalPattern.matcher(commitMessage).find()) {
        maxApprovedRev = Optional.of(rev);
      } else {
        log.info("RB {} is not approved yet, stopping examining more commits", git.getNote("reviewid"));
        break;
      }
    }

    if (!maxApprovedRev.isPresent()) {
      String tip = Seq.seq(revs).reverse().findFirst().get();
      log.info("No approved RBs found, resetting back to the tip {}", tip);
      git.resetHard(tip);
    } else {
      git.checkout("master");
      git.mergeFf(maxApprovedRev.get());
      log.info("Merged through {}, you should be able to git push now", maxApprovedRev.get());
    }
  }

}
