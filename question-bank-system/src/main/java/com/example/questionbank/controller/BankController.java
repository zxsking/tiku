package com.example.questionbank.controller;

import com.example.questionbank.common.Result;
import com.example.questionbank.common.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.BankService;
import com.example.questionbank.service.FavoriteService;
import com.example.questionbank.service.FollowService;
import com.example.questionbank.service.UserService;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Category;
import com.example.questionbank.entity.User;
import com.example.questionbank.dto.request.CreateBankRequest;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.CategoryMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "题库模块")
@RestController
@RequestMapping("/banks")
public class BankController {

    @Autowired
    private BankService bankService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FollowService followService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BankMapper bankMapper;

    @Operation(summary = "分页查询题库")
    @GetMapping
    public Result<PageResult<Bank>> getBanks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false, name = "pageSize") Integer pageSize,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "latest") String sortBy) {

        // 前端可能传 pageSize，优先使用
        int actualSize = (pageSize != null) ? pageSize : size;
        IPage<Bank> result = bankService.getBanks(page, actualSize, categoryId, keyword, status, sortBy);

        // 填充 author、categoryName 虚拟字段
        for (Bank bank : result.getRecords()) {
            User author = userService.getUserById(bank.getAuthorId());
            if (author != null) {
                bank.setAuthor(author.getUsername());
            }
            Category category = categoryMapper.selectById(bank.getCategoryId());
            if (category != null) {
                bank.setCategoryName(category.getName());
            }
        }

        PageResult<Bank> pageResult = PageResult.of(result.getTotal(), (int)result.getCurrent(), (int)result.getSize(), result.getRecords());
        return Result.success(pageResult);
    }

    @Operation(summary = "题库详情")
    @GetMapping("/{id}")
    public Result<Bank> getBank(@PathVariable Integer id,
                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 解析可选 token，获取 userId（未登录或 token 无效时为 null）
        Integer userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.replace("Bearer ", "");
                userId = jwtUtil.getUserIdFromToken(token).intValue();
            } catch (Exception ignored) {
                // 忽略无效 token，按未登录处理
            }
        }

        Bank bank = bankService.getBankById(id, userId);

        User author = userService.getUserById(bank.getAuthorId());
        if (author != null) {
            bank.setAuthor(author.getUsername());
        }
        Category category = categoryMapper.selectById(bank.getCategoryId());
        if (category != null) {
            bank.setCategoryName(category.getName());
        }
        QueryWrapper<Bank> authorBankWrapper = new QueryWrapper<>();
        authorBankWrapper.eq("author_id", bank.getAuthorId());
        bank.setAuthorBankCount(bankMapper.selectCount(authorBankWrapper));

        bank.setIsFavorited(false);
        bank.setIsFollowed(false);
        if (userId != null) {
            bank.setIsFavorited(favoriteService.isFavorited(userId, id, "bank"));
            bank.setIsFollowed(followService.isFollowing(userId, bank.getAuthorId()));
        }

        bankService.updateViewCount(id);
        return Result.success(bank);
    }

    @Operation(summary = "创建题库")
    @PostMapping
    public Result<Bank> createBank(@RequestHeader("Authorization") String authHeader,
                                   @Valid @RequestBody CreateBankRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        Bank bank = bankService.createBank(request, userId.intValue());
        return Result.success(bank);
    }

    @Operation(summary = "更新题库")
    @PutMapping("/{id}")
    public Result<Bank> updateBank(@PathVariable Integer id,
                                   @RequestHeader("Authorization") String authHeader,
                                   @Valid @RequestBody CreateBankRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        Bank bank = bankService.updateBank(id, request, userId.intValue());
        return Result.success(bank);
    }

    @Operation(summary = "删除题库")
    @DeleteMapping("/{id}")
    public Result<String> deleteBank(@PathVariable Integer id,
                                   @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        bankService.deleteBankById(id, userId.intValue());
        return Result.success("题库删除成功");
    }

    @Operation(summary = "收藏/取消收藏题库")
    @PostMapping("/{id}/favorite")
    public Result<String> toggleFavorite(@PathVariable Integer id,
                                         @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        bankService.toggleFavorite(userId.intValue(), id, "bank");
        return Result.success("操作成功");
    }
    @Operation(summary = "获取我的题库列表")
    @GetMapping("/banks")
    public Result<PageResult<Bank>> getMyBanks(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility) {

        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        IPage<Bank> result = bankService.getMyBanks(userId.intValue(), page, size, status, visibility);
        PageResult<Bank> pageResult = PageResult.of(
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize(),
                result.getRecords()
        );
        return Result.success(pageResult);
    }
}