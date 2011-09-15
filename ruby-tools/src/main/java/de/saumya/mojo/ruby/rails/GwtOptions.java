/**
 * 
 */
package de.saumya.mojo.ruby.rails;

public class GwtOptions {
    
    final String packageName;
    
    final boolean session;
    
    final boolean menu;
    
    public GwtOptions(String packageName, boolean session, boolean menu){
        this.packageName = packageName;
        this.session = session;
        this.menu = menu;
    }
}