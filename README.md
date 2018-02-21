

# Payment Tracker
Payment Tracker reads payments and every minute gives net amounts (traffic) for them.

## Input
* input file (as the first program arg if provided) with initial payments in format:
```
EUR 122
CZK 1.22
``` 
Currency code (ISO 4217), space and amount is considered as one payment. Each payment should be on its own line.
* user / pasted input into console in the same format as input file
* Main.java file stores `API` URL to exchange rate service, sign up for free 
https://currencylayer.com/signup?plan=1 and insert provided `API key`.
If you don't fill the key, the program will run, but respective USD amounts for each other currency could not be calculated.
Anyway jar file contains `API key` for ease of use.

### Invalid input
* any invalid payment (user's input / input file) is silently ignored and only valid payments are processed
* when there are more then one program arg, the program will print an error message.

## Output
After one minute and every minute the program prints net amounts (traffic) for provided payments. 
Net amounts are also expressed in relation to USD exchange rate (in brackets) except for USD itself.

## Requirements
* Java 9 SDK
* gson 2.8.2
* JUnit 5 (for tests)

## How to run
Inside project folder:
```
java -jar release/payment-tracker.jar input.txt 
```

## Tests
All major methods are unit tested, some of them could be private, but for ease of testing they are public (no need for ugly reflection).
