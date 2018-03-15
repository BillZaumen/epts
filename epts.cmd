@echo off
if -%1-==-- (
    javaw -jar EPTS_DIR/epts.jar
) else (
    if %1==--gui  (
            if not -%2-==-- (
                javaw -jar EPTS_DIR/epts.jar %*
	    ) else (
	        java -jar EPTS_DIR/epts.jar %*
            )
    ) else (
        java -jar EPTS_DIR/epts.jar %*
    )
)
