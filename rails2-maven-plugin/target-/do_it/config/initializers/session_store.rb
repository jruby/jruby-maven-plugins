# Be sure to restart your server when you modify this file.

# Your secret key for verifying cookie session data integrity.
# If you change this key, all old sessions will become invalid!
# Make sure the secret is at least 30 characters and all random, 
# no regular words or you'll be exposed to dictionary attacks.
ActionController::Base.session = {
  :key         => '_do_it_session',
  :secret      => 'b64ae40eec75185732b528f6c2bda83f0543e292d7ec99953f340c649a6374f23b97925561ef55928713f2e695bdb4fccfca919a0190b4646c5c87c403041843'
}

# Use the database for sessions instead of the cookie-based default,
# which shouldn't be used to store highly confidential information
# (create the session table with "rake db:sessions:create")
# ActionController::Base.session_store = :active_record_store
