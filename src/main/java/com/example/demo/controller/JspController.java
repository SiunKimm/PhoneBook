package com.example.demo.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.Member;
import com.example.demo.entity.Phone;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PhoneRepository;
import com.example.demo.service.Service;

@Controller
public class JspController {
	
	// Service 연결
	@Autowired
	Service service;

	// Member Repository 연결
	@Autowired
	MemberRepository repository;

	// Phone Repository 연결
	@Autowired
	PhoneRepository repository2;

	// JSP 메인 페이지
	@RequestMapping("/jsp")
	public String jsp() throws Exception {
		
		service.typedQuery();
		
		return "main";
	}

	// 메인 \ 지인 목록 페이지
	@RequestMapping("/jsp/list")
	public String getList(Model model, @RequestParam(name = "page", defaultValue = "1") Integer page,
			@RequestParam(name = "rows", defaultValue = "10") Integer rows,
			@RequestParam(name = "search_keyword", defaultValue = "") String searchKeyword) throws Exception {
		
		// page = 각 rows 를 묶는 하나의 페이지
		// rows = 한 페이지에 보여줄의 행의 수를 표현
		// Sort.by createdDate(생성일) 기준으로 정렬 및 desc 내림차순 (asce 오름차순)
		
		PageRequest pageRequest = PageRequest.of((page > 0 ? page - 1 : 0), rows, Sort.by("createdDate").descending());

		Page<Member> pageResult = null;
		// Page<Phone> pageResult2 = null;

		// 검색 키워드 여부에 따라 조회, ContainsIgnoreCase 는 해당 값의 포함 여부로 검색된다
		// 번호 조회는 findBy 를 이용하여 동일한 데이터를 찾는다
		if (searchKeyword.isEmpty()) {
			pageResult = repository.findAll(pageRequest);
		} else {
			pageResult = repository.findByFrndNmContainsIgnoreCase(searchKeyword, pageRequest);
			// pageResult2 = repository2.findByTelNo3(searchKeyword2, pageRequest);
		}

		// 현재 페이지 =pageResult 값을 Pageable, PageNumber 을 통해 판단
		int pageNumber = pageResult.getPageable().getPageNumber();

		// 총 페이지 = pageResult 값을 getTotalPages 로 총 페이지
		int totalPages = pageResult.getTotalPages();

		// 페이지 블록의 숫자를 표현, c 태그의 begin 과 end 를 사용하여 표시
		int pageBlock = 5;

		// 시작 페이지 숫자 = 현재 페이지가 7 이라면 (1*5)+1=6 시작 페이지는 6 으로 표시
		int startBlockPage = ((pageNumber) / pageBlock) * pageBlock + 1;

		// 끝 페이지 숫자 = 6+5-1=10. 6,7,8,9,10해서 10 끝 페이지는 10으로 표시
		int endBlockPage = startBlockPage + pageBlock - 1;
		endBlockPage = totalPages < endBlockPage ? totalPages : endBlockPage;
		
		//pageResult 를 ulist 로 표시, 시작과 끝 페이지 숫자를 model 로 표시
		model.addAttribute("startBlockPage", startBlockPage);
		model.addAttribute("endBlockPage", endBlockPage);
		model.addAttribute("ulist", pageResult);

		// 현재 페이지 (테스트)
		model.addAttribute("currentPage", pageResult.getNumber() + 1);

		// 전체 페이지 수 (끝)
		model.addAttribute("totalPages", pageResult.getTotalPages());

		// 전체 데이터 수
		model.addAttribute("totalRecords", pageResult.getTotalElements());

		// 이전 페이지 존재 유무
		model.addAttribute("preRecords", pageResult.hasPrevious());

		// 다음 페이지 존재 유무
		model.addAttribute("firRecords", pageResult.hasNext());

		// 조회된 레코드 목록
		model.addAttribute("list", pageResult.toList());
		return "list";
	}

	// 번호 \ 개별 지인의 전화 목록 페이지
	@RequestMapping("/jsp/detail")
	public String getDetail(Model model, @RequestParam("frndSeq") long frndSeq) throws Exception {

		// 전달 받은 멤버 ID 로 멤버를 조회
		Member member = repository.findById(frndSeq).get(0);
		model.addAttribute("member", member);

		// 조회된 멤버의 전체 번호를 getPhones() 메서드로 호출
		List<Phone> phones = member.getPhones();
		model.addAttribute("list", phones);

		return "detail";
	}

	// 메인 \ 지인 등록 페이지
	@RequestMapping("/jsp/register")
	public String openRegisterForm() throws Exception {

		return "register";
	}

	// 메인 \ 지인 등록 컨트롤러
	// 멤버 엔티티의 값, 폰 엔티티의 값들을 받아서 멤버, 폰으로 개별 저장 (OneToMany / ManyToOne 구현)
	@RequestMapping("/jsp/addMember")
	public String addMember(HttpServletRequest request) {

		String frndNm = request.getParameter("frnd_nm");
		String telType = request.getParameter("tel_type");
		String telNo1 = request.getParameter("tel_no_1");
		String telNo2 = request.getParameter("tel_no_2");
		String telNo3 = request.getParameter("tel_no_3");

		Member member = new Member(frndNm);
		repository.save(member);
		
		// 멤버의 엔티티는 저장하고 if 절을 통해서 telNo 의 크기를 비교
		// 만약 아무 번호도 없이 저장을 누르면 멤버의 엔티티만만 저장
		
		if (telNo1.length() >= 1 && telNo2.length() >= 1 && telNo3.length() >= 1 ) {
		Phone phone = new Phone(telType, telNo1, telNo2, telNo3);
		phone.setMember(member);
		repository2.save(phone);
		}

		return "redirect:/jsp/list";
	}

	// 번호 \ 전화 등록 페이지
	@RequestMapping("/jsp/enroll")
	public String openEnrollForm(Model model, @RequestParam("frndSeq") long frndSeq) throws Exception {

		model.addAttribute("member", repository.findById(frndSeq).get(0));
		return "enroll";
	}

	// 번호 \ 전화 등록 컨트롤러
	// 지정된 멤버의 번호를 추가, 번호는 멤버에 의존성을 가지고 있으므로 멤버 ID 를 필수적으로 제공
	// frndSeq 번호를 입력 받고 멤버를 조회 후 Phone Entity 를 생성, 저장하는 과정
	@RequestMapping("/jsp/addNumber")
	public String addNumber(HttpServletRequest request) {
		String frndSeq = request.getParameter("frndSeq");
		long memberId = Long.valueOf(frndSeq);

		Member member = repository.findById(memberId).get(0);
		if (member != null) {
			String telType = request.getParameter("tel_type");
			String telNo1 = request.getParameter("tel_no_1");
			String telNo2 = request.getParameter("tel_no_2");
			String telNo3 = request.getParameter("tel_no_3");

			Phone phone = new Phone(telType, telNo1, telNo2, telNo3);
			phone.setMember(member);
			repository2.save(phone);
		}
		return "redirect:/jsp/detail?frndSeq=" + frndSeq;
	}

	// 메인 \ 지인 수정 페이지
	@RequestMapping("/jsp/modify")
	public String openModifyForm(Model model, @RequestParam("frndSeq") long frndSeq) throws Exception {

		model.addAttribute("member", repository.findById(frndSeq).get(0));
		return "modify";
	}

	// 메인 \ 지인 수정 컨트롤러
	// 멤버 엔티티의 frndSeq 값을 가져와서 이름을 수정, 저장
	@RequestMapping("/jsp/updateMember")
	public String updateMember(HttpServletRequest request) {

		long frndSeq = Integer.parseInt(request.getParameter("frndSeq"));
		String frndNm = request.getParameter("frnd_nm");

		Member member = repository.findById(frndSeq).get(0);
		member.setFrndNm(frndNm);
		repository.save(member);

		return "redirect:/jsp/detail?frndSeq=" + frndSeq;
	}

	// 번호 \ 지인 전화 수정 페이지
	@RequestMapping("/jsp/numodify")
	public String opennuModifyForm(Model model, @RequestParam("telSeq") long telSeq) throws Exception {

		model.addAttribute("member", repository2.findById(telSeq).get(0));
		return "numodify";
	}

	// 번호 \ 지인 전화 수정 컨트롤러
	// telSeq 값을 가져와서 폰 엔티티 데이터를 수정, 저장
	@RequestMapping("/jsp/updatenuMember")
	public String updatenuMember(HttpServletRequest request) {

		long telSeq = Integer.parseInt(request.getParameter("telSeq"));
		String telType = request.getParameter("tel_type");
		String telNo1 = request.getParameter("tel_no_1");
		String telNo2 = request.getParameter("tel_no_2");
		String telNo3 = request.getParameter("tel_no_3");

		Phone phone = repository2.findById(telSeq).get(0);
		phone.setTelType(telType);
		phone.setTelNo1(telNo1);
		phone.setTelNo2(telNo2);
		phone.setTelNo3(telNo3);
		repository2.save(phone);

		return "redirect:/jsp/list";

	}

	// 메인 \ 지인 삭제 컨트롤러
	@RequestMapping("/delete")
	public String deleteMember(@RequestParam("frnd_seq") long frndSeq) {

		repository.deleteById(frndSeq);

		return "redirect:/jsp/list";
	}

	// 번호 \ 지인 전화 삭제 컨트롤러
	@RequestMapping("/ddelete")
	public String ddeleteMember(@RequestParam("tel_seq") long telSeq) {

		repository2.deleteById(telSeq);

		return "redirect:/jsp/list";
	}

}
