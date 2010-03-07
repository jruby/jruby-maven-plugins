SNAPSHOT=$1
VERSION=$2
NEXT=$3
if [ "x" = "x"$2 ] ; then
    echo "usage: $0 SNAPHOT_VERSION VERSION NEXT_VERSION"
    exit 1
fi

# first run the complete tests
mvn3 clean install -Pintegration-tests || exit 1

find -name "pom.xml" | xargs sed -i s/${SNAPSHOT}-SNAPSHOT/${VERSION}/
find -name "pom.xml" | xargs git add
git ci -m "release of version ${VERSION}" || exit
git tag v${VERSION}

mvn3 clean deploy || exit 2

find -name "pom.xml" | xargs sed -i s/${VERSION}/${NEXT}-SNAPSHOT/

find -name "pom.xml" | xargs git add
git ci -m "next snapshot version ${NEXT}"

mvn3 install || exit 3

git push --tags origin master
