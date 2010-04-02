# Be sure to restart your server when you modify this file.

# Your secret key for verifying cookie session data integrity.
# If you change this key, all old sessions will become invalid!
# Make sure the secret is at least 30 characters and all random, 
# no regular words or you'll be exposed to dictionary attacks.
ActionController::Base.session = {
  :key         => '_do_it2_session',
  :secret      => '98938d5f78decf2fcf187af598f24e74419da827db18a701dada8bbb53b2cc7a82da62468e99c00d86deafffd9a73fd15f8bbb89f8a400dea3d239ebe28d7d9e'
}

# Use the database for sessions instead of the cookie-based default,
# which shouldn't be used to store highly confidential information
# (create the session table with "rake db:sessions:create")
# ActionController::Base.session_store = :active_record_store
