const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)];
// Áreas
const AR=[["Construção Civil","construcao-civil"],["Indústria","industria"],["Varejo","varejo"],["Serviços","servicos"],["Agronegócio","agronegocio"],["Saúde","saude"],["Educação","educacao"],["Logística","logistica"],["Finanças","financas"],["Energia e Utilities","energia-utilities"],["Alimentos e Bebidas","alimentos-bebidas"]];
$('#ar').innerHTML=AR.map(([n,f],i)=>`<li class="${i%3==1&&i<9?'dn':''} ${i==9?'a':''} ${i==10?'b':''}"><button class="ar" aria-pressed="${i==0}"><img src="img/${f}.png" alt="">${n}</button></li>`).join('');
$$('.ar').forEach(b=>b.onclick=()=>{const w=b.getAttribute('aria-pressed')=='true';$$('.ar').forEach(x=>x.setAttribute('aria-pressed','false'));b.setAttribute('aria-pressed',String(!w))});
// Rotas
const V=['home','empresa','login','cadastro','eva'];
function route(){let h=location.hash.slice(1)||'login',sec=null;if(h=='areas'){h='home';sec='areas'}if(!V.includes(h))h='home';
$$('.view').forEach(v=>v.classList.toggle('on',v.id=='v-'+h));
$('.hd').style.display=h=='eva'?'none':'';$('#top').style.display=(h=='home'||h=='empresa')?'':'none';$('#ft').style.display=h=='eva'?'none':'';$('#fab').style.display=h=='eva'?'none':'';
$('#nav').classList.remove('o');$('#mt').setAttribute('aria-expanded','false');
sec?document.getElementById(sec).scrollIntoView():scrollTo(0,0)}
addEventListener('hashchange',route);route();
// Menu e dropdown
$('#mt').onclick=()=>{const o=$('#nav').classList.toggle('o');$('#mt').setAttribute('aria-expanded',o)};
// Abas
$$('.tb').forEach(b=>b.onclick=()=>{$$('.tb').forEach(x=>x.setAttribute('aria-selected','false'));b.setAttribute('aria-selected','true');$$('.tp').forEach((p,i)=>p.classList.toggle('on',i==b.dataset.t))});

// ==========================================
// INTEGRAÇÃO COM O BACKEND SPRING BOOT
// ==========================================

// Cadastro de Usuário
$('#cf').onsubmit = async e => {
    e.preventDefault();
    
    // Pega os valores dos inputs do formulário de cadastro (ajuste os seletores se necessário)
    const emailInput = $('#cf input[type="email"]') || $('#cf input[name="email"]');
    const senhaInput = $('#cf input[type="password"]') || $('#cf input[name="senha"]');
    
    const dados = {
        email: emailInput ? emailInput.value : '',
        senha: senhaInput ? senhaInput.value : ''
    };

    try {
        const resposta = await fetch('/api/auth/cadastrar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dados)
        });

        const mensagem = await resposta.text();

        if (resposta.ok) {
            $('#cm').textContent = 'Cadastro realizado com sucesso! Pode fazer login.';
            $('#cm').style.color = 'green';
            setTimeout(() => { location.hash = '#login'; }, 2000);
        } else {
            $('#cm').textContent = mensagem || 'Erro ao cadastrar.';
            $('#cm').style.color = 'red';
        }
    } catch (erro) {
        $('#cm').textContent = 'Erro de conexão com o servidor Java.';
        $('#cm').style.color = 'red';
    }
};

// Login de Usuário
$('#lf').onsubmit = async e => {
    e.preventDefault();

    const emailInput = $('#lf input[type="email"]') || $('#lf input[name="email"]');
    const senhaInput = $('#lf input[type="password"]') || $('#lf input[name="senha"]');

    const dados = {
        email: emailInput ? emailInput.value : '',
        senha: senhaInput ? senhaInput.value : ''
    };

    try {
        const resposta = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dados)
        });

        const mensagem = await resposta.text();

        if (resposta.ok) {
            alert('Login efetuado com sucesso!');
            location.hash = '#home'; // Redireciona para a home após logar
        } else {
            alert(mensagem || 'Email ou senha incorretos!');
        }
    } catch (erro) {
        alert('Erro de conexão com o servidor Java.');
    }
};

// Chat: a IA sera integrada por outro grupo.
const bot=$('#ms').querySelector('img').src;
window.evaResponder=t=>{const m=document.createElement('div');m.className='m';m.innerHTML='<img alt="EVA"><span></span>';m.firstChild.src=bot;m.lastChild.textContent=t;$('#ms').append(m);m.scrollIntoView({block:'end'})};
$('#chf').onsubmit=e=>{e.preventDefault();const i=$('#ci'),t=i.value.trim();if(!t)return;
const u=document.createElement('div');u.className='m u';u.innerHTML='<span></span>';u.firstChild.textContent=t;$('#ms').append(u);i.value='';
dispatchEvent(new CustomEvent('eva:enviar',{detail:t}))};
// Aviso de suporte
try{if(sessionStorage.getItem('av')!='1')setTimeout(()=>$('#av').classList.add('on'),1500)}catch(e){setTimeout(()=>$('#av').classList.add('on'),1500)}
$('#avx').onclick=()=>{$('#av').classList.remove('on');try{sessionStorage.setItem('av','1')}catch(e){}};