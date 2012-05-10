describe "error in before clause" do

  before :all do
    nil.unknown
  end

  it "should succeed" do
  end

end
