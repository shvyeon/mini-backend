package org.zerock.mallapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.zerock.mallapi.domain.Member;
import org.zerock.mallapi.dto.MemberDTO;
import org.zerock.mallapi.dto.MemberModifyDTO;
import org.zerock.mallapi.repository.MemberRepository;

import java.util.LinkedHashMap;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Log4j2
public class MemberServiceImpl implements MemberService{
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public MemberDTO getKakaoMember(String accessToken){
        String email = getEmailFromKakaoAccessToken(accessToken);

        log.info("email: {}", email);

        Optional<Member> result = memberRepository.findById(email);

        //기존의 회원
        if(result.isPresent()){
            return entityToDTO(result.get());
        }

        // 회원이 아니었다면
        // 닉네임은 '소셜회원' 으로
        // 패스워드는 임의로 생성
        Member socialMember = makeSocialMember(email);
        memberRepository.save(socialMember);

        return entityToDTO(socialMember);
    }

    private String getEmailFromKakaoAccessToken(String accessToken) {

        String kakaoGetUserURL ="https://kapi.kakao.com/v2/user/me";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization","Bearer "+ accessToken);
        headers.add("Content-type","application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(kakaoGetUserURL).build();

        ResponseEntity<LinkedHashMap> response =
                restTemplate.exchange(uriComponents.toString(), HttpMethod.GET, entity, LinkedHashMap.class);

        log.info("response..................");
        log.info(response);

        LinkedHashMap<String, LinkedHashMap> bodyMap = response.getBody();

        LinkedHashMap<String, String> kakaoAccount = bodyMap.get("properties");

        log.info("kakaoAccount: {}", kakaoAccount);
        String nickname = kakaoAccount.get("nickname");
        log.info("nickname: {}", nickname);

        return nickname;
    }

    // 패스워드 임의 생성 (소셜 로그인용)
    private String makeTempPassword(){
        StringBuffer buffer = new StringBuffer();

        for(int i=0;i<10;i++){
            buffer.append((char) ((int)(Math.random()*55) + 65));
        }

        return buffer.toString();
    }

    // 이메일이 존재하지 않을때 Member 객제 생성 (소셜 로그인용)
    private Member makeSocialMember(String email){
        String tempPassword = makeTempPassword();
        log.info("tempPassword: {}", tempPassword);
        String nickname = "소셜회원";

        return Member.builder()
                .email(email)
                .pw(passwordEncoder.encode(tempPassword))
                .nickname(nickname)
                .social(true)
                .build();
    }

    @Override
    public void modifyMember(MemberModifyDTO memberModifyDTO){
        Optional<Member> result = memberRepository.findById(memberModifyDTO.getEmail());

        Member member = result.orElseThrow();

        member.changePw(passwordEncoder.encode(memberModifyDTO.getPw()));
        member.changeSocial(false);
        member.changeNickname(memberModifyDTO.getNickname());

        memberRepository.save(member);
    }
}
