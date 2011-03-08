<?xml version="1.0"?>
<xsl:transform 
    xmlns:p="http://maven.apache.org/POM/4.0.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text" encoding="utf-8"/>

  <xsl:template match="/">
    <xsl:text>#-*- mode: ruby -*-
Gem::Specification.new do |s|
</xsl:text>
    <xsl:apply-templates select="project/*"/>
    <xsl:text>end
</xsl:text>
  </xsl:template>

  <xsl:template match="project/artifactId">
    <xsl:text>  s.name = '</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="project/version">
    <xsl:text>  s.version = '</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="project/name">
    <xsl:text>  s.summary = '</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="project/description">
    <xsl:text>  s.description = '</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="project/url">
    <xsl:text>  s.homepage = '</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="developer[1]" priority="1">
    <xsl:text>  s.authors = ['</xsl:text>
    <xsl:value-of select="name"/>
    <xsl:text>']
</xsl:text>
    <xsl:text>  s.email = ['</xsl:text>
    <xsl:value-of select="email"/>
    <xsl:text>']
</xsl:text>
  </xsl:template>

  <xsl:template match="license">
    <xsl:text>  s.licenses += ['</xsl:text>
    <xsl:value-of select="name"/>
    <xsl:text>']
</xsl:text>
  </xsl:template>

  <xsl:template match="developer">
    <xsl:text>  s.authors += ['</xsl:text>
    <xsl:value-of select="name"/>
    <xsl:text>']
</xsl:text>
    <xsl:text>  s.email += ['</xsl:text>
    <xsl:value-of select="email"/>
    <xsl:text>']
</xsl:text>
  </xsl:template>

  <xsl:template match="project/build/plugins/plugin[artifactId='gem-maven-plugin']" priority="1">
    <xsl:apply-templates select="configuration/*"/>
    <xsl:if test="executions/execution/configuration/files">
      <xsl:text>  s.files += [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="configuration/files | executions/execution/configuration/files"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="hasRdoc">
      <xsl:text>  s.has_rdoc = </xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>
</xsl:text>
  </xsl:template>

  <xsl:template match="bindir">
      <xsl:text>  s.bindir = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="rubyforgeProject">
      <xsl:text>  s.rubyforge_project = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="requiredRubygemsVersion">
      <xsl:text>  s.required_rubygems_version = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="requiredRubyVersion">
      <xsl:text>  s.required_ruby_version = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template
>
  <xsl:template match="platform">
      <xsl:text>  s.platform = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="postInstallMessage">
      <xsl:text>  s.post_install_message = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="autorequire">
      <xsl:text>  s.autorequire = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="defaultExecutable">
      <xsl:text>  s.default_executable = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="bindir">
      <xsl:text>  s.bindir = '</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>'
</xsl:text>
  </xsl:template>

  <xsl:template match="rdocOptions">
      <xsl:text>  s.rdoc_options = [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="text()"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
  </xsl:template>

  <xsl:template match="executables">
      <xsl:text>  s.executables = [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="text()"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
  </xsl:template>

  <xsl:template match="extensions">
      <xsl:text>  s.extensions = [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="text()"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
  </xsl:template>

  <xsl:template match="requirePaths">
      <xsl:text>  s.require_paths = [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="text()"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
  </xsl:template>

  <xsl:template match="requirements">
      <xsl:text>  s.requirements = [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="text()"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
  </xsl:template>

  <xsl:template match="files">
      <xsl:text>  s.files = [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="text()"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
  </xsl:template>

  <xsl:template match="testFiles">
      <xsl:text>  s.test_files = [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="text()"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
  </xsl:template>

  <xsl:template match="extraRdocFiles">
      <xsl:text>  s.extra_rdoc_files = [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="text()"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
  </xsl:template>

  <xsl:template name="split">
    <xsl:param name="val"/>

    <xsl:choose>
      <xsl:when test="contains($val, ',')">
	<xsl:text>'</xsl:text>
	<xsl:value-of select="substring-before($val, ',')"/>
	<xsl:text>',</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="substring-after($val, ',')"/>
      </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<xsl:text>'</xsl:text>
	<xsl:value-of select="$val"/>
	<xsl:text>'</xsl:text>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template match="project/build/plugins/plugin[artifactId='rspec-maven-plugin']" priority="1">
    <xsl:if test="configuration/files or executions/execution/configuration/files">
      <xsl:text>  s.files += [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="configuration/files | executions/execution/configuration/files"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="project/build/plugins/plugin[artifactId='runit-maven-plugin']" priority="1">
    <xsl:if test="configuration/files or executions/execution/configuration/files">
      <xsl:text>  s.files += [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="configuration/files | executions/execution/configuration/files"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="project/build/plugins/plugin[artifactId='cucumber-maven-plugin']" priority="1">
    <xsl:if test="configuration/files or executions/execution/configuration/files">
      <xsl:text>  s.files += [</xsl:text>
      <xsl:call-template name="split">
	<xsl:with-param name="val" select="configuration/files | executions/execution/configuration/files"/>
      </xsl:call-template>
      <xsl:text>]
</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="dependencyManagement"/>

  <xsl:template match="dependencies">
    <xsl:apply-templates select="*"/>
  </xsl:template>

  <xsl:template match="dependency[type = 'gem']">
    <xsl:variable name="artifactId" select="artifactId"/>
    <xsl:text>  s.add_</xsl:text>
    <xsl:if test="scope = 'test' or scope = 'provided'">
      <xsl:text>development_</xsl:text>
    </xsl:if>
    <xsl:text>dependency '</xsl:text>
    <xsl:value-of select="$artifactId"/>
    <xsl:text>', '</xsl:text>
    <xsl:choose>
      <xsl:when test="version">
	<xsl:value-of select="version"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="/project/dependencyManagement/dependency[artifactId = $artifactId]/version"/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>'
</xsl:text>
  </xsl:template>


  <xsl:template match="dependency[not(starts-with(groupId, 'org.jruby'))]" mode="maven"/>
  
  <xsl:template match="dependency[starts-with(groupId, 'org.jruby')]"/>
  		
  <xsl:template match="version[text() = '[0.0.0,)']" mode="nested"/>
  <xsl:template match="version[contains(text(), '.99999)')]" mode="nested">
    <xsl:text>, '~> </xsl:text>
    <xsl:value-of select="substring-before(substring-after(text(), '['), ',')"/>
    <xsl:text>'</xsl:text>
  </xsl:template>
  
  <xsl:template match="version" mode="nested">
    <xsl:text>, '</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>'</xsl:text>
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