package com.simbaquartz.xapi.connect.api.account.factory;

import com.simbaquartz.xapi.connect.api.account.AccountApiService;
import com.simbaquartz.xapi.connect.api.account.impl.AccountApiServiceImpl;

public class AccountApiServiceFactory {

    private static final AccountApiService service = new AccountApiServiceImpl();

    public static AccountApiService getAccountApi() {
        return service;
    }
}
