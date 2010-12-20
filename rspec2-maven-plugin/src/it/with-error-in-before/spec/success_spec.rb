describe "error in before spec" do

  before :all do
    nil.unknown
  end

  it "should succeed" do
  end

end
