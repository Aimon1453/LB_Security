# Report of Lab1-TOCTOU

## P0

Here is the code that we add to complete the application. We use "if and else" to compare the price of product and the balance. If balance is not enough,then we fail to buy the product, if it is enough, we compute the "newBalance" and set it, add the product to our pocket. It is simple, but here is a sleep code(5 seconds) before we set the new balance, this is for us to attack this application easier. Set break points is also feasible, principle is same, we will talk this in the next part.

```java
    while(!product.equals("quit")) {
            /* TODO:
               - check if the amount of credits is enough, if not stop the execution.
               - otherwise, withdraw the price of the product from the wallet.
               - add the name of the product to the pocket file.
               - print the new balance.
            */
            if(wallet.getBalance()<Store.getProductPrice(product)){
                System.out.println("balance not enough！");
            }
            else{
                int newBalance=wallet.getBalance()-Store.getProductPrice(product);

                Thread.sleep(5000);//wait 5 seconds

                wallet.setBalance(newBalance);
                pocket.addProduct(product);
            }
            // Just to print everything again...
            print(wallet, pocket);
            product = scan(scanner);
        }
```

## P1

In this part, we use two terminals, and enter this application. In the A terminal, we choose to buy "car", and in the B terminal, we choose to buy "book". In the end, we get the car and book successfully, and the balance is 29900. Follow is the figure
![A terminal](image.png)
![B terminal](image-1.png)

### What is the shared resource? Who is sharing it? 

wallet.txt(current credit balance) is the shared resource. It can be accessed and modified by both terminals(threads), so it is shared between them through the frontend APIs.

### What is the root of the problem?

The wallet update operation is not atomic. There is a race condition between: Reading the current balance (wallet.getBalance()), Modifying it, and Writing back the new balance (wallet.setBalance(...)).

When two terminals perform this sequence in parallel without synchronization, one write can overwrite the other — leading to lost updates and an incorrect final balance.

### Explain in detail how you can attack this system.

we buy the car first, since we use sleep code to delay the wallet.setBalance(...) operation, so the balance will be changed after 5 seconds, that's means if we buy the other product in another terminal, the application is still think we have 30000 credits, this is why we can buy book even if we actually cannot. After 5 seconds, the A terminal setBalance to 0(the value has been computed before delay), but after that the B terminal execute the setBalance, and the value is 29900(the value is 30000-100).

While the TOCTOU vulnerability can be exploited without any artificial delays, but it requires very precise timing. Therefore, to reliably demonstrate the issue, we use artificial delays (Thread.sleep()) to widen the race window and ensure the vulnerability is observable.

### Provide the program output and result, explaining the interleaving to achieve them.

You can see the output of the program in the figure above.

1. A reads balance = 30000
2. A judge the balance is enough to buy the car
3. A comupte the newBalance=0, and sleep
4. B reads balance = 30000
5. B judge the balance is enough to buy the book
6. B compute the newBalance=29900, and sleep
...wait for A wake
7. A setBalance to 0
...wait for B wake
8. B setBalance to 29900
   
Result: Both items purchased, we spent money on a book and bought a book and a car

## P2

We implemented a new method in the Wallet class:
```java 
     private final Lock lock = new ReentrantLock();  
```
```java
    public boolean safeWithdraw(int valueToWithdraw) throws Exception {
        //optimize，return directly if balance not enough, avoid lock's overhead
        if (getBalance() < valueToWithdraw) {
            return false;
        }

        lock.lock();
        try {
            int currentBalance = getBalance();
            if (currentBalance >= valueToWithdraw) {
                int newBalance = currentBalance - valueToWithdraw;
                setBalance(newBalance);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock(); // unlock, if successful or not
        }
    }
```

This method ensures that checking and modifying the wallet balance happen atomically, using a lock mechanism. It prevents other threads from modifying the balance in the middle of the operation, which eliminates the TOCTOU race condition exploited in Part 1.

### Were there other APIs or resources suffering from possible races?

Yes, another possible race resource in the system is the pocket.txt file, written via the Pocket.addProduct() method.
Since multiple frontend instances may write to this file concurrently, it is vulnerable to race conditions similar to the one found in Wallet.

It is almost same with wallet.txt we discuss before, so the method is similar, we just lock the addProduct():
```java 
private final Lock lock = new ReentrantLock();  
```

```java
  public void addProduct(String product) throws Exception {
        lock.lock(); 
        try {
            this.file.seek(this.file.length());
            this.file.writeBytes(product + '\n');
        } finally {
            lock.unlock(); 
        }
    }
```

### Why are these protections enough and at the same time not too excessive? 

Our protection is enough because it guarantees atomicity during balance updates, we use locks to prevent threads from accessing the possible race data simultaneouslyfully, preventing the TOCTOU race exploited in Part 1.

It's not excessive because We only lock in a small area, we avoided adding locks to unrelated APIs. In the method "safeWithdraw()", we will check whether the balance is sufficient before adding the lock, and if it is sufficient, we will add the lock again. At the same time, after adding the lock again, we will compare the balance again to see if it is sufficient. Although there is an additional criterion for judgment under normal circumstances, if the amount is insufficient, there is no need to lock it at all, avoiding the cost of locking.

Here is the optimize part, the complete is above
``` java 
   //optimize，return directly if balance not enough, avoid lock's overhead
        if (getBalance() < valueToWithdraw) {
            return false;
        }

        lock.lock();
        try {
            ......
            }
        } finally {
            lock.unlock(); // unlock, if successful or not
        }
```

## conlusion

Through the above implementation, we not only eliminated the occurrence of toctou vulnerabilities, but also to some extent avoided the cost of locking.