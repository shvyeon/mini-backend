package org.zerock.mallapi.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.mallapi.dto.PageRequestDTO;
import org.zerock.mallapi.dto.PageResponseDTO;
import org.zerock.mallapi.dto.ProductDTO;
import org.zerock.mallapi.service.ProductService;
import org.zerock.mallapi.util.CustomFileUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/products")
public class ProductController {
    private final CustomFileUtil fileUtil;
    private final ProductService productService;

    // 파일 업로드
    @PostMapping("/")
    public Map<String, Long> register(ProductDTO productDTO){
        log.info("resigter : " + productDTO);

        List<MultipartFile> files = productDTO.getFiles();
        List<String> uploadFileNames = fileUtil.saveFiles(files);

        productDTO.setUploadFileNames(uploadFileNames);

        log.info(uploadFileNames);

        Long pno = productService.register(productDTO);

        return Map.of("result", pno);
    }

    // 파일 경로
    @GetMapping("/view/{fileName}")
    public ResponseEntity<Resource> viewFileGET(@PathVariable String fileName){

        return fileUtil.getFile(fileName);
    }

    //  @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/list")
    public PageResponseDTO<ProductDTO> list(PageRequestDTO pageRequestDTO){
        log.info("list.................  " + pageRequestDTO);

        return productService.getList(pageRequestDTO);
    }

    @GetMapping("/{pno}")
    public ProductDTO read(@PathVariable(name = "pno") Long pno){
        return productService.get(pno);
    }

    @PutMapping("/{pno}")
    public Map<String, String> modify(@PathVariable(name = "pno") Long pno, ProductDTO productDTO){
        productDTO.setPno(pno);
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println(productDTO);
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        ProductDTO oldProductDTO = productService.get(pno);
        // 기존 파일들
        List<String> oldFileNames = oldProductDTO.getUploadFileNames();
        // 새로 업로드 할것들
        List<MultipartFile> files = productDTO.getFiles();
        // 새로 업로드 된 파일 이름들
        List<String> currentUploadFileNames = fileUtil.saveFiles(files);
        // 유지되는 애들
        List<String> uploadedFileNames = productDTO.getUploadFileNames();

        // 새로 업로드 + 기존 유지
        if(currentUploadFileNames != null && !currentUploadFileNames.isEmpty()){
            uploadedFileNames.addAll(currentUploadFileNames);
        }

        // 수정
        productService.modify(productDTO);

        if(oldFileNames != null && !oldFileNames.isEmpty()){
            // 지워야 하는 파일 목록 찾기
            List<String> removeFiles = oldFileNames.stream()
                    .filter(fileName -> !uploadedFileNames.contains(fileName))
                    .toList();

            // 실제 파일 삭제
            fileUtil.deleteFiles(removeFiles);
        }

        return Map.of("RESULT", "SUCCESS");
    }

    @DeleteMapping("/{pno}")
    public Map<String, String> remove(@PathVariable("pno") Long pno){
        List<String> oldFileNames = productService.get(pno)
                .getUploadFileNames();

        productService.remove(pno);
        fileUtil.deleteFiles(oldFileNames);

        return Map.of("RESULT", "SUCCESS");
    }
}
