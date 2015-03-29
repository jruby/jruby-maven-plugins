package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/**
 * installs a set of given gems without resolving any transitive dependencies
 * 
 * @goal jars-lock
 * @phase initialize
 * @requiresDependencyResolution test
 */
public class JarsLockMojo extends AbstractMojo {

    private static final String JARS_HOME = "JARS_HOME";

	/**
     * reference to maven project for internal use.
     *
     * @parameter expression="${project}"
     * @required
     * @readOnly
     */
    protected MavenProject project;

    /**
     * Jars.lock file to be updated or created.
     *
     * <br/>
     * Command line -Djars.lock=...
     * 
     * @parameter expression="${jars.lock}" default-value="Jars.lock"
     */
    public File jarsLock;

    /**
     * where to copy the jars - default to JARS_HOME environment if set.
     *
     * <br/>
     * Command line -Djars.home=...
     * 
     * @parameter expression="${jars.home}"
     */
    public File jarsHome;

    /**
     * force update of Jars.lock file.
     *
     * <br/>
     * Command line -Djars.force=...
     * 
     * @parameter expression="${jars.force}" default-value="false"
     */
    public boolean force;

    /**
     * update of Jars.lock file for a given artifactId
     *
     * <br/>
     * Command line -Djars.update=...
     * 
     * @parameter expression="${jars.update}"
     */
    public String update;

    /** @component */
    protected RepositorySystem repositorySystem;
    
    /**
     * local repository for internal use.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
    	if ( update != null ) {
    		updateArtifact();
    	}
    	else {
    		processJarsLock();
    	}
    }

    void processJarsLock() throws MojoExecutionException {
    	List<String> lines = toLines( project.getArtifacts() );
    	
    	try {
        	
    		switch ( needsForcedUpdate(lines) ) {
    		case NEEDS_FORCED_UPDATE:
    			getLog().info( jarsLock() + " has outdated dependencies" );
    			break;
    		case CAN_UPDATE:
    			updateJarsLock(lines);
    			break;
    		case UP_TO_DATE:
    			getLog().info( jarsLock() + " is up to date" );
    			// ensure jars are vendored
    			vendorJars();
    		default:
    		}
    		
    	} catch (IOException e) {
    		throw new MojoExecutionException( "can not read " + jarsLock, e );
		}
    }

	private void updateJarsLock(List<String> lines) throws MojoExecutionException {
		String action = jarsLock.exists() ? "updated" : "created";
		try {
			writeJarsLock( lines );
		}
		catch (IOException e) {
    		throw new MojoExecutionException( "can not write " + jarsLock, e );
		}
		try {
			// vendor new jars
			vendorJars();
		}
		catch (IOException e) {
    		throw new MojoExecutionException( "can not vendor jars from " + jarsLock, e );
		}
		getLog().info( jarsLock() + " " + action );
	}

	private List<String> toLines( Set<Artifact> artifacts ) {
		List<String> lines = new LinkedList<String>();
    	for ( Artifact a: artifacts ) {
    		String line = toLine( a );
    		if ( line != null ) lines.add( line );
    	}
		return lines;
	}

	private void updateArtifact() throws MojoExecutionException {
		ArtifactResolutionResult result = resolve();
		if ( result == null ) {
			getLog().error( "no such artifact in " + jarsLock() + ": " + update );
		}
		else if ( result.isSuccess() ) {
			for ( Artifact a: result.getArtifacts() ) {
				if ( a.getArtifactId().equals( update ) ) {
					getLog().info( "updated " + a );
					break;
				}
			}
			updateJarsLock( toLines( result.getArtifacts() ) );
		}
		else {
			for ( Exception e: result.getExceptions() ) {
				getLog().error( e.getMessage() );
			}
			for ( Artifact a: result.getMissingArtifacts() ) {
				getLog().error( "missing artifact: " + a );
			}
		}
	}

	private ArtifactResolutionResult resolve() throws MojoExecutionException {
		List<String> jars = loadJarsLock();
		ArtifactResolutionRequest request = new ArtifactResolutionRequest();
		Set<Artifact> artifacts = new HashSet<Artifact>();
		boolean hasUpdate = false;
		for( String jar: jars) {
			Artifact a = createArtifact(jar);
			if ( a != null ) {
				if ( a.getArtifactId().equals( update ) ) {
					try {
						a.setVersionRange(VersionRange.createFromVersionSpec( "[" + a.getVersion() + ",)" ) );
					}
					catch (InvalidVersionSpecificationException e) {
						throw new RuntimeException("something wrong with creating version range", e);
					}
					hasUpdate = true;
				}
				artifacts.add( a );
			}
		}
		if ( !hasUpdate ) return null;
		
		request.setArtifactDependencies(artifacts);
		request.setResolveTransitively(false);
		request.setResolveRoot(false);
		request.setArtifact(project.getArtifact());
		request.setLocalRepository(localRepository);
		request.setRemoteRepositories(project.getRemoteArtifactRepositories());
		ArtifactResolutionResult result = repositorySystem.resolve( request );
		return result;
	}

	private Artifact createArtifact(String jar) {
		if ( !jar.endsWith( ":" ) || jar.startsWith( "#" ) ) return null;
		String[] parts = jar.split( ":" );
		if ( parts.length == 4 ) { 
			return repositorySystem.createArtifact( parts[0], 
				parts[1], parts[2],  parts[3], "jar" );
		}
		if ( parts.length == 5 ) { 
			Artifact a = repositorySystem.createArtifactWithClassifier( parts[0], 
					parts[1], parts[3], "jar", parts[2] );
			a.setScope( parts[4] );
			return a;
		}
		getLog().warn( "ignore :" + jar );
		return null;
	}
    
    private String jarsLock() {
    	return jarsLock.getAbsolutePath().replace( project.getBasedir().getAbsolutePath() + File.separator, 
    											   "" );
    }

	private void vendorJars() throws IOException {
		if ( jarsHome == null ) {
			if ( System.getenv( JARS_HOME ) != null ) {
				jarsHome = new File( System.getenv( JARS_HOME ) );
			}
		}
		if ( jarsHome != null ) {
			jarsHome.mkdirs();
		}
		if ( jarsHome == null || ! jarsHome.exists() || ! jarsHome.isDirectory() ) {
			return;
		}
    	for( Artifact a: project.getArtifacts() ) {
        	if ( a.getType().equals( "jar" ) && ! a.getScope().equals( Artifact.SCOPE_SYSTEM ) ) {
        		File target = new File( jarsHome, a.getGroupId().replace(".", File.separator) + File.separator +
        				a.getArtifactId() + File.separator + a.getVersion() + File.separator +
        				a.getFile().getName() );
        		if (force || a.getFile().length() != target.length() ) {
        			getLog().debug( "* vendor " + a );
        			FileUtils.copyFile( a.getFile(), target );
        		}
        		else {
        			getLog().debug( "* up to date " + a );
        		}
        	}
    	}
	}

	private void writeJarsLock(List<String> lines) throws FileNotFoundException {
		PrintWriter out = null;
		try {
			out = new PrintWriter( jarsLock );
			for( String line : lines ) {
				out.println( line );
			}
		}
		finally {
			IOUtil.close( out );
		}
	}

	private static enum Status {
		CAN_UPDATE, NEEDS_FORCED_UPDATE, UP_TO_DATE
	}
	
	private Status needsForcedUpdate(List<String> lines) throws IOException, MojoExecutionException {
		if ( force ) {
			return Status.CAN_UPDATE;
		}
		if ( jarsLock.exists() ) {
    		Set<String> newLines = new TreeSet<String>( lines );
    		Set<String> oldLines = new TreeSet<String>( loadJarsLock() );
    		if ( newLines.containsAll(oldLines)) {
    			return oldLines.containsAll( newLines ) ? Status.UP_TO_DATE : Status.CAN_UPDATE;
    		}
    		return Status.NEEDS_FORCED_UPDATE;
		}
		return Status.CAN_UPDATE;
	}

	@SuppressWarnings("unchecked")
	private List<String> loadJarsLock() throws MojoExecutionException {
		try {
			return FileUtils.loadFile( jarsLock );
		}
		catch ( IOException e ) {
			throw new MojoExecutionException( "can not read " + jarsLock, e );
		}
	}

	private String toLine( Artifact a ) {
    	if ( !a.getType().equals( "jar" ) ) return null;
    	StringBuilder line = new StringBuilder( a.toString().replace( ":jar:", ":") );
    	line.append( ":" );
    	if ( a.getScope().equals( Artifact.SCOPE_SYSTEM ) ) {
    		line.append( getSystemFile( a.getFile().getPath() ) );
    	}
    	return line.toString();
    }

    private String getSystemFile( String file ) {
    	for( Entry<Object, Object> prop: System.getProperties().entrySet() ) {
    		String key = prop.getKey().toString();
    		String value = prop.getValue().toString();
    		int index = file.indexOf( value );
    		if ( index > -1 && new File( value ).isDirectory() && 
    				!"file.separator".equals( key ) ) {
    			return file.replace( value, "${" + key + "}" );
    		}
    	}
    	return "";
    }
}