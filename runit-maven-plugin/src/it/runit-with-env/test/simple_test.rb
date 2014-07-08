class SimpleTest < Test::Unit::TestCase

  def test_it
     assert ENV['VERSION'] == '123'
  end
 
end
