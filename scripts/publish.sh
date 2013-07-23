#!/bin/bash
set -eu
: ${1:? Usage: $0 DESCRIPTION}
DESCRIPTION="$1"
set -x

# TODO: release OSSRH and push to GitHub automatically
#mvn nexus-staging:release \
#    --errors \
#    -DaltStagingDirectory=staging \
#    -DstagingDescription="$DESCRIPTION"

set +x
echo ""
echo "Done. Next steps:"
echo "    open https://oss.sonatype.org/"
echo "    git push origin HEAD"
echo "    git push origin --tags"
