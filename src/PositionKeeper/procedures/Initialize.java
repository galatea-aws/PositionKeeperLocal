/* This file is part of VoltDB.
 * Copyright (C) 2008-2013 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

//
// Initializes the database, pushing the list of contestants and documenting domain data (Area codes and States).
//

package PositionKeeper.procedures;

import org.voltdb.ProcInfo;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

@ProcInfo (
    singlePartition = false
)
public class Initialize extends VoltProcedure
{
    // Check if the database has already been initialized
    public final SQLStmt checkStmt = new SQLStmt("SELECT COUNT(*) FROM accounts;");

    // Inserts an account
    public final SQLStmt insertAccountStmt = new SQLStmt("INSERT INTO accounts (account_id) VALUES (?);");

    // Inserts a product
    public final SQLStmt insertProductStmt = new SQLStmt("INSERT INTO products (product_cusip, product_name) VALUES (?, ?);");


    public long run(int maxAccounts, int maxProducts) {
        voltQueueSQL(checkStmt, EXPECT_SCALAR_LONG);
        long existingAccount = voltExecuteSQL()[0].asScalarLong();

        // if the data is initialized, return the account count
        if (existingAccount != 0)
            return existingAccount;

        // initialize the data
        String accountId = null;
        for (int i=1; i <= maxAccounts; i++){
        	accountId = "account" + i;
            voltQueueSQL(insertAccountStmt, EXPECT_SCALAR_MATCH(1), accountId);
        }
        voltExecuteSQL();
        
        String productCusip = null;
        String productName = null;
        for (int i=0; i < maxProducts; i++){
        	productCusip = "cusip" + i;
        	productName = "product" + i;
            voltQueueSQL(insertProductStmt, EXPECT_SCALAR_MATCH(1), productCusip, productName);
        }   
        voltExecuteSQL();

        return maxAccounts;
    }
}
