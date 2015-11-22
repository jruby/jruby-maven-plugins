package de.saumya.mojo.mavengem.wagon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;

import de.saumya.mojo.mavengem.RubygemsFactory;
import de.saumya.mojo.mavengem.MavenGemURLConnection;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;

public class MavenGemWagon extends StreamWagon {

    public static final String MAVEN_GEM_PREFIX = "mavengem:";

    private Proxy proxy = Proxy.NO_PROXY;

    // configurable via the settings.xml
    private File cachedir;
    private String mirror;

    private void warn(String msg) {
	System.err.println("WARNING: " + msg);
    }

    @Override
    public void fillInputData(InputData inputData)
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

        Resource resource = inputData.getResource();

	try {

	    if (proxy != Proxy.NO_PROXY) {
		warn("proxy support is not implemented - ignoring proxy settings");
	    }

	    RubygemsFactory factory;
	    // use some default if not set
	    if (cachedir == null) {
		cachedir = RubygemsFactory.DEFAULT_CACHEDIR;
	    }
	    if (mirror != null) {
		factory = new RubygemsFactory(cachedir, withAuthentication(mirror));
	    }
	    else {
		factory = new RubygemsFactory(cachedir);
	    }
	    URLConnection urlConnection = new MavenGemURLConnection(factory,
								    getRepositoryURL(),
								    "/" + resource.getName());
	    InputStream is = urlConnection.getInputStream();
	
	    inputData.setInputStream(is);
	    resource.setLastModified(urlConnection.getLastModified());
	    resource.setContentLength(urlConnection.getContentLength());

	}
        catch(MalformedURLException e) {
	    throw new ResourceDoesNotExistException("Invalid repository URL: " + e.getMessage(), e);
        }
        catch(FileNotFoundException e) {
	    throw new ResourceDoesNotExistException("Unable to locate resource in repository", e);
        }
        catch(IOException e) {
            throw new TransferFailedException("Error transferring file: " + e.getMessage(), e);
        }
    }

    @Override
    public void fillOutputData(OutputData outputData)
        throws TransferFailedException {
	throw new RuntimeException("only download is provided");
    }

    @Override
    public void closeConnection()
        throws ConnectionException {
    }

    private URL withAuthentication(String url)  throws MalformedURLException {
	if (authenticationInfo != null && authenticationInfo.getUserName() != null) {
	    String credentials = authenticationInfo.getUserName() + ":" + authenticationInfo.getPassword();
	    url = url.replaceFirst("^(https?://)(.*)$", "$1" + credentials + "@$2");
	}
	return new URL(url);
    }

    private URL getRepositoryURL() throws MalformedURLException {
	String url = getRepository().getUrl().substring(MAVEN_GEM_PREFIX.length());
	return withAuthentication(url);
    }

    private Proxy getProxy(ProxyInfo proxyInfo) {
        return new Proxy(getProxyType(proxyInfo), getSocketAddress(proxyInfo));
    }

    private Type getProxyType(ProxyInfo proxyInfo) {
        if (ProxyInfo.PROXY_SOCKS4.equals(proxyInfo.getType()) || ProxyInfo.PROXY_SOCKS5.equals(proxyInfo.getType())) {
            return Type.SOCKS;
        }
        else {
            return Type.HTTP;
        }
    }

    private SocketAddress getSocketAddress(ProxyInfo proxyInfo) {
        return InetSocketAddress.createUnresolved(proxyInfo.getHost(), proxyInfo.getPort());
    }

    @Override
    protected void openConnectionInternal()
        throws ConnectionException, AuthenticationException {

	try {
	    final ProxyInfo proxyInfo = getProxyInfo(getRepositoryURL().getProtocol(), getRepository().getHost());
	    if (proxyInfo != null) {
		this.proxy = getProxy( proxyInfo );
	    }
	}
	catch (MalformedURLException e) {
	    throw new ConnectionException("cannot create repository url", e);
	}
    }
}
