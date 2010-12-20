describe "succeeding spec" do

  it "should succeed" do
    logger = org.slf4j.LoggerFactory.getLogger("spec")
    logger.should_not be_nil
    logger.info("logging something very important")
  end

end
