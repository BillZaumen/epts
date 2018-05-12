<?xml version="1.0"  encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
  <html>
    <body>
	<xsl:for-each select="toc/node">
	  <h2> <A target="manual">
	    <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
	    <xsl:value-of select="@title"/></A></h2>
	  <xsl:for-each select="node">
	    <ul>
	      <li><A target="manual">
		    <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
		    <xsl:value-of select="@title"/>
	        </A>
	      <xsl:for-each select="node">
		<ul>
		  <li><A target="manual">
		    <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
		    <xsl:value-of select="@title"/>
		  </A></li>
		</ul>
	      </xsl:for-each>
	      </li>
	    </ul>
	  </xsl:for-each>
	</xsl:for-each>
    </body>
  </html>
</xsl:template>
</xsl:stylesheet>
