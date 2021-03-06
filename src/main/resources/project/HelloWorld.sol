pragma solidity ^0.4.25;

// Modified Greeter contract. Based on example at https://www.ethereum.org/greeter.

contract Mortal {
    /* Define variable owner of the type address*/
    address owner;

    /* this function is executed at initialization and sets the owner of the contract */
    constructor () public { owner = msg.sender; }

    /* Function to recover the funds on the contract */
    function kill() public { if (msg.sender == owner) selfdestruct(owner); }
}

contract HelloWorld is Mortal {
    /* define variable greeting of the type string */
    string greet;

    /* this runs when the contract is executed */
    constructor (string _greet) public {
        greet = _greet;
    }

    function newGreeting(string _greet) public {
        greet = _greet;
    }

    /* main function */
    function greeting() public constant returns (string)  {
        return greet;
    }
}