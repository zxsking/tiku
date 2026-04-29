package com.example.questionbank.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.dto.request.CreateBankRequest;

public interface BankService {
    IPage<Bank> getBanks(Integer page, Integer size, Integer categoryId, String keyword, String status, String sortBy);
    Bank getBankById(Integer id);
    Bank getBankById(Integer id, Integer userId);
    Bank createBank(CreateBankRequest request, Integer userId);
    Bank updateBank(Integer id, CreateBankRequest request, Integer userId);
    void deleteBankById(Integer id, Integer userId);
    void updateViewCount(Integer bankId);
    void toggleFavorite(Integer userId, Integer targetId, String targetType);
    IPage<Bank> getBanksForReview(Integer page, Integer size, String status);
    Bank reviewBank(Integer id, String status);

    IPage<Bank> getMyBanks(Integer userId, Integer page, Integer size, String status);
    IPage<Bank> getMyBanks(Integer userId, Integer page, Integer size, String status, String visibility);
}