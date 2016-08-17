
review-branch
=============

`review-branch` is a script to turn 1 local branch into multiple ReviewBoard code reviews.

It is smart enough to:

* Create initial RBs for your branch, 1 RB per commit
  * On initial RB creation, it will set the reviewers, groups, testing-done, etc. on each RB
* Update RBs after you've made changes (e.g. based on RB feedback), and intelligently update the original RB only if needed
  * E.g. if you had commits `A -> B -> C` and do a fix up to `B` to make `A -> B' -> C` and run `review-branch review`, it will update the RB that was initally created for `B` and then not touch the RBs for `A` or `C` to avoid spurious emails
  * Note that if you drop `B` completely, and now have `A -> C`, `review-branch` currently won't go close `B's` now-defunct RB; this is doable, just not implemented yet

It currently assumes an internal environment, because instead of running the `rbt` tool, it uses an internal `git review` command, although in theory it could be adapted to use the raw `rbt` command itself; using `git review` was just the shortest path for me.

Usage
=====

On your local branch, create code reviews by running:

    review-branch review [--reviewers bob,fred] [--groups my-reviewers] [--publish] [--testing-done "yep!"]

After you've made updates to your commits, or added new commits, run `review-branch review` again, and it will update RBs only for changed commits.

After you get ship it's, stamp each of your commits:

    review-branch dcommit

Then merge your branch to master (e.g. `git merge --ff local-branch`) and `git push`.

Install from source
===================

1. Clone this repo
2. Run `./gradlew shadowJar`
3. Copy `build/libs/review-branch-all.jar` and `review-branch` to your home directory (or `~/bin` or other misc tools directory on your path)
4. `chmod u+x ~/review-branch`
5. Now run `~/review-branch review` or `~/review-branch dcommit` in your project's directory

Install from pre-built jar
==========================

1. Download [review-branch-all.jar](http://repo.joist.ws/review-branch-all.jar) and [review-branch](http://repo.joist.ws/review-branch) to your home directory (or `~/bin` or other misc tools directory)
2. `chmod u+x review-branch`
3. Now run `~/review-branch review` or `~/review-branch dcommit` in your project's directory

