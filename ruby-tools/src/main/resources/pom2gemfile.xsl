<?xml version="1.0"?>
<xsl:transform 
    xmlns:p="http://maven.apache.org/POM/4.0.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text" encoding="utf-8"/>

  <xsl:template match="/">
    <xsl:text>#-*- mode: ruby -*-
source 'http://rubygems.org'

</xsl:text>
     <xsl:apply-templates select="p:project/*"/>
<xsl:text>
if defined?(MAVEN)
</xsl:text>
    <xsl:call-template name="profile"/>
    <xsl:apply-templates select="p:project/p:profiles/p:profile[p:properties|p:build/p:plugins]" 
			 mode="maven">
      <xsl:with-param name="indent">  
	<xsl:text>  </xsl:text>
      </xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>end
</xsl:text>
  </xsl:template>

  <xsl:template name="profile">
    <xsl:param name="indent"/>
    <xsl:apply-templates select="p:project/p:dependencies/p:dependency| p:project/p:properties | p:project/p:build/p:plugins/p:plugin|p:dependency|p:properties|p:build/p:plugins/p:plugin" 
			 mode="maven">
      <xsl:with-param name="indent">  
	<xsl:value-of select="$indent"/>
	<xsl:text>  </xsl:text>
      </xsl:with-param>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="p:profile" mode="maven">
    <xsl:param name="indent"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>group :</xsl:text>
    <xsl:value-of select="p:id"/>
    <xsl:text> do
</xsl:text>
    <xsl:call-template name="profile">
      <xsl:with-param name="indent">
	<xsl:value-of select="$indent"/>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:value-of select="$indent"/>
    <xsl:text>end
</xsl:text>
  </xsl:template>

  <xsl:template match="p:properties" mode="maven">
    <xsl:param name="indent"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>properties({
</xsl:text>
    
    <xsl:apply-templates select="*" 
			 mode="prop">
      <xsl:with-param name="indent">  
	<xsl:text>  </xsl:text>
	<xsl:value-of select="$indent"/>
      </xsl:with-param>
    </xsl:apply-templates>
    <xsl:value-of select="$indent"/>
    <xsl:text>})
</xsl:text>
  </xsl:template>

  <xsl:template match="*" mode="prop">
    <xsl:param name="indent"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>'</xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:text>' => '</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>',
</xsl:text>
  </xsl:template>

  <xsl:template match="p:dependency" mode="maven">
    <xsl:param name="indent"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>jar '</xsl:text>
    
    <xsl:value-of select="groupId"/>
    <xsl:text>.</xsl:text>
    <xsl:value-of select="p:artifactId"/>
    <xsl:text>'</xsl:text>
    <xsl:apply-templates select="p:version" mode="nested"/>
    <xsl:text>
</xsl:text>
  </xsl:template>

  <xsl:template match="p:plugin" mode="maven">
    <xsl:param name="indent"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>plugin '</xsl:text>
    <xsl:apply-templates select="p:groupId" mode="maven"/>
    <xsl:apply-templates select="p:artifactId" mode="maven"/>
    <xsl:text>'</xsl:text>
    <xsl:apply-templates select="p:version" mode="nested"/>
    <xsl:text>
</xsl:text>
  </xsl:template>

  <xsl:template match="p:plugin[p:groupId = 'de.saumya.mojo']" mode="maven">
    <xsl:param name="indent"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>plugin '</xsl:text>
    <xsl:value-of select="substring-before(p:artifactId, '-maven-plugin')"/>
    <xsl:text>'</xsl:text>
    <xsl:apply-templates select="p:version" mode="nested"/>
    <xsl:text>
</xsl:text>
  </xsl:template>

  <xsl:template match="p:groupId" mode="maven">
    <xsl:value-of select="text()"/>
    <xsl:text>.</xsl:text>
  </xsl:template>

  <xsl:template match="p:artifactId[starts-with(text(), 'maven-') and substring-after(substring-after(text(), '-'), '-') = 'plugin']" mode="maven">
    <xsl:value-of select="substring-before(substring-after(text(), 'maven-'), '-plugin')"/>
  </xsl:template>

  <xsl:template match="p:artifactId" mode="maven">
    <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="p:dependency[not(starts-with(p:groupId, 'org.jruby'))]" mode="maven"/>
  
  <xsl:template match="p:dependency[starts-with(p:groupId, 'org.jruby')] | p:plugin"/>
  		
  <xsl:template match="p:dependency">
    <xsl:param name="indent"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>gem '</xsl:text>
    <xsl:choose>
      <xsl:when test="p:type = 'gem'">
	<xsl:value-of select="p:artifactId"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="p:groupId"/>
	<xsl:text>.</xsl:text>
	<xsl:value-of select="p:artifactId"/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>'</xsl:text>
    <xsl:apply-templates select="p:version" mode="nested"/>
    <xsl:text>
</xsl:text>
  </xsl:template>

  <xsl:template match="p:version[text() = '[0.0.0,)']" mode="nested"/>
  <xsl:template match="p:version[contains(text(), '.99999.99999)')]" mode="nested">
    <xsl:text>, '~> </xsl:text>
    <xsl:value-of select="substring-before(substring-after(text(), '['), ',')"/>
    <xsl:text>'</xsl:text>
  </xsl:template>
  
  <xsl:template match="p:version" mode="nested">
    <xsl:text>, '</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>'</xsl:text>
  </xsl:template>

  <xsl:template match="p:profile[p:dependencies]">
    <xsl:param name="indent"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>group :</xsl:text>
    <xsl:value-of select="id"/>
    <xsl:text> do
</xsl:text>
    <xsl:apply-templates select="@*|*">
      <xsl:with-param name="indent">
	<xsl:value-of select="$indent"/>
	<xsl:text>  </xsl:text>
      </xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>end
</xsl:text>
  </xsl:template>

  <xsl:template match="*" priority="-1">
    <xsl:param name="indent"/>
    <xsl:apply-templates select="@*|*">
      <xsl:with-param name="indent">
	<xsl:value-of select="$indent"/>
      </xsl:with-param>
    </xsl:apply-templates>
  </xsl:template>
</xsl:transform>