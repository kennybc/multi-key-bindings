CHANGELOG=$(awk "/^## $MOD_VERSION /{flag=1;next}/^## /{flag=0}flag" CHANGELOG.md | tail -n +2)
            if [ -z "$CHANGELOG" ]; then
              CHANGELOG="Release for Minecraft $b"
            fi