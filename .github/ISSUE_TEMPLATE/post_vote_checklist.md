---
name: Post Vote Checklist
about: A checklist for tracking post-vote release tasks
title: 'Post-vote release checklist for version [e.g. 1.0.0]'
labels:
assignees: ''

---

- [ ] Label this issue with the GitHub [milestone](https://github.com/apache/accumulo-access/milestones) that corresponds to this release version
- [Git](https://github.com/apache/accumulo-access) tasks
  - [ ] Create a signed `rel/<version>` tag (and push it)
  - [ ] Merge `<version>-rc<N>-next` branch into a maintenance branch (if maintenance is expected),
        and then into the `main` branch (and push them)
  - [ ] Remove `*-rc*` branches
- [Nexus](https://repository.apache.org) tasks
  - [ ] Release the staging repository corresponding to the successful release candidate (use "release" button)
  - [ ] Drop any other staging repositories for Accumulo-Access (do *not* "release" or "promote" them)
- [SVN / dist-release](https://dist.apache.org/repos/dist/release/accumulo-access) tasks
  - [ ] Upload the release artifacts (tarballs, signatures, and `.sha512` files) for the mirrors
  - [ ] Remove old artifacts from the mirrors (**after** updating the website)
- [Board reporting tool](https://reporter.apache.org/addrelease?accumulo)
  - [ ] Add the date of release (the date the release artifacts were uploaded to SVN `UTC+0000`)
- Verify published artifacts
  - [ ] In [Maven Central](https://repo1.maven.org/maven2/org/apache/accumulo/accumulo-access/)
  - [ ] In [ASF Downloads](https://downloads.apache.org/accumulo)
  - [ ] In [several mirrors or CDN](https://www.apache.org/dyn/closer.lua/accumulo)
- Update the [website](https://accumulo.apache.org/)
  - [ ] Release notes
  - [ ] Navigation
  - [ ] Downloads page
  - [ ] If LTM release, update previous LTM release entry on downloads page and release notes with an EOL date 1 year from the current release date
  - [ ] DOAP file
- Announcement email
  - [ ] Prepare and get review on dev list (see examples [from previous announcement messages](https://lists.apache.org/list.html?announce@apache.org:gte=1d:accumulo))
  - [ ] Send to announce@apache.org and user@accumulo.apache.org (use plain text mode only; html email will be rejected)
- GitHub wrap-up
  - [ ] Close this issue
  - [ ] Create a new GitHub milestone for the next version (if necessary) and move any open issues not completed in this release to that project
  - [ ] Close the milestone that corresponds to this release

