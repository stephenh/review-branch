package reviewbranch.commands;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Test;

import reviewbranch.apis.Git;
import reviewbranch.apis.ReviewBoard;

  @SuppressWarnings("unchecked")
public class DCommitCommandTest {

  private final Git git = mock(Git.class);
  private final ReviewBoard rb = mock(ReviewBoard.class);
  private final ReviewCommand args = new ReviewCommand();

  @After
  public void after() {
    verifyNoMoreInteractions(git, rb);
  }

  @Test
  public void dcommitTwoCommits() {
    // given we want to dcommit two new commits
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"), Optional.of("2"));
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of("hash"));
    when(git.getCurrentCommit()).thenReturn("commitA2");
    // when ran
    new DCommitCommand().run(git, rb);
    // then we dcommit each commit
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getCurrentCommit();
    verify(git).resetHard("commitB");
    verify(git).resetHard("commitA2");
    verify(git).cherryPick("commitB");
    verify(git, atLeast(3)).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", "hash");
    verify(rb).dcommit("1");
    verify(rb).dcommit("2");
  }

  @Test
  public void setupGitNotes() {
    args.ensureGitNotesConfigured(git);
    verify(git, atLeast(2)).getMultipleValueConfig("notes.displayRef");
    verify(git, atLeast(2)).getMultipleValueConfig("notes.rewriteRef");
    verify(git).addMultipleValueConfig("notes.displayRef", "refs/notes/reviewid");
    verify(git).addMultipleValueConfig("notes.displayRef", "refs/notes/reviewlasthash");
    verify(git).addMultipleValueConfig("notes.rewriteRef", "refs/notes/reviewid");
    verify(git).addMultipleValueConfig("notes.rewriteRef", "refs/notes/reviewlasthash");
  }

}
