// Validações e máscaras usadas nos formulários (o servidor valida novamente).
export const soDigitos = (t) => (t || '').replace(/\D/g, '');

export function cpfValido(cpf) {
  const d = soDigitos(cpf);
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false;
  const digito = (tam) => {
    let soma = 0;
    for (let i = 0; i < tam; i++) soma += Number(d[i]) * (tam + 1 - i);
    const r = (soma * 10) % 11;
    return r === 10 ? 0 : r;
  };
  return digito(9) === Number(d[9]) && digito(10) === Number(d[10]);
}

export const emailValido = (e) => /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/.test((e || '').trim());
export const telefoneValido = (t) => [10, 11].includes(soDigitos(t).length);
export const senhaForte = (s) => (s || '').length >= 8 && /[A-Za-z]/.test(s) && /\d/.test(s);

export function mascaraCpf(valor) {
  const d = soDigitos(valor).slice(0, 11);
  return d.replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}

export function mascaraTelefone(valor) {
  const d = soDigitos(valor).slice(0, 11);
  if (d.length <= 2) return d ? '(' + d : '';
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

/** Liga máscaras aos inputs marcados com data-mask="cpf" ou data-mask="telefone" dentro do elemento. */
export function ativarMascaras(raiz) {
  raiz.querySelectorAll('[data-mask]').forEach((input) => {
    const fn = input.dataset.mask === 'cpf' ? mascaraCpf : mascaraTelefone;
    input.addEventListener('input', () => { input.value = fn(input.value); });
    input.value = fn(input.value);
  });
}
