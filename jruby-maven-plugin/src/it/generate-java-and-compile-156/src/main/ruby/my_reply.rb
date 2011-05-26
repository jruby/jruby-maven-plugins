require 'java'
java_import 'com.example.Reply'
java_package 'com.otherexample'

class MyReply
  java_implements Reply

  java_signature "String reply()"
  def reply
    "may all be happy"
  end
end
