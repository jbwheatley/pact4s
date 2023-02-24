# Publishing our test pacts

As part of the testing process of this library, we publish pacts to a public pact broker managed by the pact foundation expressly for
the purpose of testing pact integrations. To update these I (Jack) do the following: 

1. Run the pact forger suites in one of the testing implementations. 
2. Copy the produced pact files from the `target` directory into the corresponding files in the `scripts` directory. 
3. Run the `publish_pacts.sh` script. 

This is an inherently flawed system because we want to run forger and verification suites for the same pacts simultaneously in CI, so 
before anyone pushes a feature branch with changes to these tests they would have to publish the updated pacts, probably breaking the 
existing CI in the process. This has worked well enough when it has just been me adding feature tests to these pacts, but eventually it 
will cause headaches. 

There is likely a way we could automate around this - run only the forger tests, publish the pacts tagged with the feature branch, run only 
the verification tests. Hopefully at some point someone will find the time to try this out! 